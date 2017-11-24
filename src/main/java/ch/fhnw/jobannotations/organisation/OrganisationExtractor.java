package ch.fhnw.jobannotations.organisation;

import ch.fhnw.jobannotations.Main;
import ch.fhnw.jobannotations.utils.FileUtils;
import ch.fhnw.jobannotations.utils.PartOfSpeechUtil;
import com.aliasi.dict.TrieDictionary;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrganisationExtractor {

    // official legal form postfixes in switzerland
    private static final String[] KNOWN_LEGAL_FORMS = {
            "AG", "Gen", "Genossenschaft", "GmbH", "KlG", "KmG", "KmAG", "SA", "SCoop", "Sàrl",
            "SNC", "SCm", "SCmA", "Sagl", "SAc", "SAcA", "Scrl", "SCl", "SACm"
    };

    public String parse(Document document) {

        if (Main.DEBUG) {
            System.out.println("\n" + StringUtils.repeat("-", 80));
            System.out.println("[organisation]\t" + "Started to parse organisation from offer");
        }

        // get the visible text from document
        // TODO: instead of body().text(); analyse TAG-texts
        final String text = document.body().text();

        // detect sentences from text
        String[] sentences = PartOfSpeechUtil.detectSentences(text);

        List<String> legalFormCandidates = getLegalFormCandidates(sentences);
        Map<String, Integer> fuzzySearchCandidates = getFuzzySearchCandidates(text);

        // ---------------------------------------------------------
        // (1) analyse indicator results
        // ---------------------------------------------------------
        if (!legalFormCandidates.isEmpty()) {

            // TODO: rate "ACTIV FITNESS AG" higher
            // [organization-indicator]	ACTIV FITNESS AG
            // [organization-indicator]	Bewerben ACTIV FITNESS AG

            Map<String, Integer> weightedIndicatorCandidates = new HashMap<>();

            int posScore = 0;
            for (String candidate : legalFormCandidates) {

                // check against found fuzzy search chunks
                for (Map.Entry<String, Integer> fuzzyCandidate : fuzzySearchCandidates.entrySet()) {

                    if (candidate.toLowerCase().contains(fuzzyCandidate.getKey().toLowerCase())) {

                        if (weightedIndicatorCandidates.containsKey(candidate)) {

                            weightedIndicatorCandidates.put(candidate, weightedIndicatorCandidates.get(candidate) + 1 - fuzzyCandidate.getValue());

                        } else {
                            weightedIndicatorCandidates.put(candidate, 1 + posScore - fuzzyCandidate.getValue());
                        }

                    }

                }

                // no cross match found for candidate, rate at least with it's position score
                if (!weightedIndicatorCandidates.containsKey(candidate)) {
                    weightedIndicatorCandidates.put(candidate, posScore);
                }

                posScore++;
            }

            return weightedIndicatorCandidates.entrySet().stream().max((a, b) -> a.getValue() > b.getValue() ? 1 : -1).get().getKey().trim();

        }

        // ---------------------------------------------------------
        // (2) analyse fuzzy chunks for perfect matches
        // ---------------------------------------------------------
        long perfectMatches = fuzzySearchCandidates.entrySet().stream().filter(a -> a.getValue() == 0).count();

        if (perfectMatches > 0) {

            String lastMatch = "";

            for (Map.Entry<String, Integer> entry : fuzzySearchCandidates.entrySet()) {

                if (entry.getValue() == 0) {
                    lastMatch = entry.getKey(); // replacing means later position in document is higher rated
                }

            }

            return lastMatch.trim();
        }

        // ---------------------------------------------------------
        // (3) analyse fuzzy and ner for cross results
        // ---------------------------------------------------------
        System.out.println("[organization] Need to look for NER - WARNING: this may take a while...");

        List<String> merged = new ArrayList<>();

        // add all fuzzy candidates keys
        fuzzySearchCandidates.entrySet().stream().forEach(t -> merged.add(t.getKey()));

        // add all ner tags found
        merged.addAll(getNerCandidates(text));

        // search for similarities
        Map<String, Integer> weighted = new HashMap<>();

        for (String candidate : merged) {

            for (String c : merged) {

                if (candidate.toLowerCase().contains(c.toLowerCase())) {

                    if (weighted.containsKey(candidate)) {

                        weighted.put(candidate, weighted.get(candidate) + 1);

                    } else {
                        weighted.put(candidate, 1);
                    }

                }

            }

        }

        if (weighted.entrySet().stream().max((a, b) -> a.getValue() > b.getValue() ? 1 : -1).isPresent()) {
            return weighted.entrySet().stream().max((a, b) -> a.getValue() > b.getValue() ? 1 : -1).get().getKey().trim();
        } else {
            return "organisation not found!";
        }
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
                            if (organisationName.trim().length() > legalForm.length() && !candidates.contains(organisationName + legalForm)) {
                                candidates.add(organisationName + legalForm);

                                System.out.println("[organization-indicator]\t" + organisationName + legalForm);
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
    private Map<String, Integer> getFuzzySearchCandidates(String text) {

        // get chunks for known organisation names which may be recognized within the text
        TrieDictionary knownCompanies = PartOfSpeechUtil.getTrieDictionaryByFile("data/known_companies.txt", "ORG");
        Map<String, Integer> foundChunks = PartOfSpeechUtil.getChunksByDictionary(knownCompanies, text, 1);

        // return found chunks as simple List<String>
        // TODO: use additional information such as score to enhance prediction
        Map<String, Integer> candidates = new HashMap<>();
        for (Map.Entry<String, Integer> entry : foundChunks.entrySet()) {
            candidates.put(entry.getKey(), entry.getValue());

            System.out.println("[organization-approx]\t" + entry.getKey());
        }

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
        try {
            pipeline.annotate(document);
        } catch (Exception e) {
            // TODO pipeline.annotate(document) can throw NullPointerException
            // also if neither pipeline nor document are null
        }

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        List<String> candidates = new ArrayList<>();

        for (CoreMap sentence : sentences) {

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            String currentOrganisation = "";

            for (int i = 0; i < tokens.size(); i++) {

                CoreLabel token = tokens.get(i);

                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                if (ne.equals("I-ORG")) {
                    currentOrganisation = currentOrganisation + " " + word;
                } else if (currentOrganisation != "") {
                    candidates.add(currentOrganisation);

                    System.out.println("[organization-ner]\t" + currentOrganisation);

                    currentOrganisation = "";
                }

            }

        }

        return candidates;
    }

    public static String removeOrganisationFromString(String organisation, String jobTitle) {

        String organisationWithoutLegalIdentifier = organisation;

        // remove known legal identifiers
        for (String identifier : KNOWN_LEGAL_FORMS) {
            organisationWithoutLegalIdentifier = organisationWithoutLegalIdentifier.replaceAll("\\s(?i)" + identifier, "");
        }

        // replace organisation from job title
        String cleaned = jobTitle.replaceAll("(?i)" + organisation, "");
        cleaned = cleaned.replaceAll("(?i)" + organisationWithoutLegalIdentifier, "");

        return cleaned.trim();
    }


}
