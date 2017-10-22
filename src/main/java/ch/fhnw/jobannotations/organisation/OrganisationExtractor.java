package ch.fhnw.jobannotations.organisation;

import ch.fhnw.jobannotations.utils.FileUtils;
import ch.fhnw.jobannotations.utils.PartOfSpeechUtil;
import com.aliasi.dict.TrieDictionary;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrganisationExtractor {


    // official legal form postfixes in switzerland
    private final String[] KNOWN_LEGAL_FORMS = {
            "AG", "Gen", "Genossenschaft", "GmbH", "KlG", "KmG", "KmAG", "SA", "SCoop", "Sàrl",
            "SNC", "SCm", "SCmA", "Sagl", "SAc", "SAcA", "Scrl", "SCl", "SACm"
    };

    public String parse(Document document) {

        // get the visible text from document
        final String text = document.body().text();

        // detect sentences from text
        String[] sentences = PartOfSpeechUtil.detectSentences(text);

        List<String> legalFormCandidates = getLegalFormCandidates(sentences);
        List<String> fuzzySearchCandidates = getFuzzySearchCandidates(text);
        List<String> nerCandidates = getNerCandidates(text);

        return "";
    }


    /**
     * Within the passed sentence, a search for possible organisation names is performed.
     * If a probable organisation name is found, it's returned with a score from [0 - 100].
     *
     * @param sentences to analyse
     * @return probable organisation name with score
     */
    private List<String> getLegalFormCandidates(String[] sentences) {

        List<String> candidates = new ArrayList<>();

        for (String sentence : sentences) {

            // analyse sentence's part of speech structure
            String[] posTags = PartOfSpeechUtil.analysePartOfSpeech(sentence);

            // search for known legal form postfixes in sentence
            for (String legalForm : KNOWN_LEGAL_FORMS) {

                if (sentence.toLowerCase().contains(legalForm.toLowerCase())) {

                    String[] words = sentence.split(" ");

                    for (int i = 0; i < words.length; i++) {

                        if (PartOfSpeechUtil.clearWord(words[i]).toLowerCase().equals(legalForm.toLowerCase())) {

                            String organisationName = ""; // glue probable name together

                            // check words left from found postfix
                            // naive: NE NN NE Postfix == organisation name
                            for (int j = i - 1; j >= 0; j--) {
                                if (posTags[j].equals("NN") || posTags[j].equals("NE")) {

                                    // make sure word does not contain special chars
                                    Pattern p = Pattern.compile(PartOfSpeechUtil.SPECIAL_CHARS);
                                    Matcher m = p.matcher(words[j]);

                                    if (!m.find())
                                        organisationName = words[j] + " " + organisationName;

                                } else {
                                    break;
                                }
                            }

                            // build found organisation name
                            if (organisationName.trim().length() > legalForm.length()) {
                                candidates.add(organisationName + legalForm);

                                System.out.println("[organization-indicator]\t" + organisationName + " " + legalForm);
                            }

                        }
                    }

                }

            }
        }

        return candidates;
    }

    /**
     * Analyses given text with LingPipe's basic NER class
     * ApproxDictionaryChunker, similar to a basic fuzzy search.
     *
     * @param sentences to analyse for organisation names
     * @return list of probable organisation names
     */
    private List<String> getFuzzySearchCandidates(String text) {

        // get chunks for known organisation names which may be recognized within the text
        TrieDictionary knownCompanies = PartOfSpeechUtil.getTrieDictionaryByFile("data/known_companies.txt", "ORG");
        Map<String, int[]> foundChunks = PartOfSpeechUtil.getChunksByDictionary(knownCompanies, text, 1);

        // return found chunks as simple List<String>
        // TODO: use additional information such as score to enhance prediction
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : foundChunks.entrySet()) {
            candidates.add(entry.getKey());

            System.out.println("[organization-approx]\t" + entry.getKey());
        }
        // extract candidates
        return candidates;
    }

    /**
     * Analyses given text with CoreNLP NER (named entity recognition)
     * and tries to identify probable organisation names.
     *
     * @param text to analyse for organisation names
     * @return list of probable organisation names
     */
    private List<String> getNerCandidates(String text) {

        StanfordCoreNLP pipeline = new StanfordCoreNLP(FileUtils.getStanfordCoreNLPGermanConfiguration());

        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        List<String> candidates = new ArrayList<>();

        for (CoreMap sentence : sentences) {

            String lastNerTag = "";
            String organisationCandidate = "";

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            String currentOrganisation = "";

            for (int i = 0; i < tokens.size(); i++) {

                CoreLabel token = tokens.get(i);

                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                if (ne.equals("I-ORG")) {
                    currentOrganisation = currentOrganisation + " " + word;
                } else if(currentOrganisation != ""){
                    candidates.add(currentOrganisation);

                    System.out.println("[organization-ner]\t" + currentOrganisation);

                    currentOrganisation = "";
                }

            }

        }

        return candidates;
    }


}
