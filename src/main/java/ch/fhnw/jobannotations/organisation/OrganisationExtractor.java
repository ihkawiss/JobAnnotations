package ch.fhnw.jobannotations.organisation;

import ch.fhnw.jobannotations.utils.FileUtils;
import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.ApproxDictionaryChunker;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.TrieDictionary;
import com.aliasi.spell.FixedWeightEditDistance;
import com.aliasi.spell.WeightedEditDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrganisationExtractor {

    // regex to filter special characters
    private final String SPECIAL_CHARS = "[$&+,:;=?@#<>.^*%!-\"]";

    // official legal form postfixes in switzerland
    private final String[] KNOWN_LEGAL_FORMS = {
            "AG", "Gen", "", "GmbH", "KlG", "KmG", "KmAG", "SA", "SCoop", "Sàrl",
            "SNC", "SCm", "SCmA", "Sagl", "SAc", "SAcA", "Scrl", "SCl", "SACm"
    };

    public String parse(Document document) {

        // get the visible text from document
        final String text = document.body().text();

        // detect sentences from text
        String[] sentences = detectSentences(text);

        List<String> candidates = new ArrayList<>();
        candidates.addAll(getLegalFormCandidates(sentences));
        candidates.addAll(getFuzzySearchCandidates(sentences));
        candidates.addAll(getNerCandidates(text));

        for (String candidate : candidates)
            System.out.println(candidate);

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
            String[] posTags = analysePartOfSpeech(sentence);

            // search for known legal form postfixes in sentence
            for (String legalForm : KNOWN_LEGAL_FORMS) {

                if (sentence.toLowerCase().contains(legalForm.toLowerCase())) {

                    String[] words = sentence.split(" ");

                    for (int i = 0; i < words.length; i++) {

                        if (clearWord(words[i]).toLowerCase().equals(legalForm.toLowerCase())) {

                            String organisationName = ""; // glue probable name together

                            // check words left from found postfix
                            // naive: NE NE NE Postfix == organisation name
                            // TODO: implement more intelligent algorithm (which may give better results)
                            for (int j = i; j >= 0; j--) {
                                if (posTags[j].equals("NN") || posTags[j].equals("NE")) {

                                    // make sure word does not contain special chars
                                    Pattern p = Pattern.compile(SPECIAL_CHARS);
                                    Matcher m = p.matcher(words[j]);

                                    if (!m.find())
                                        organisationName = words[j] + " " + organisationName;

                                } else {
                                    break;
                                }
                            }

                            // build found organisation name
                            if (organisationName.trim().length() > legalForm.length())
                                candidates.add(organisationName);
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
    private List<String> getFuzzySearchCandidates(String[] sentences) {

        List<String> candidates = new ArrayList<>();

        try {

            TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

            WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -1, -1, -1, Double.NaN);

            double maxDistance = 2.0;

            ApproxDictionaryChunker chunker = new ApproxDictionaryChunker(getKnownOrganisations(), tokenizerFactory, editDistance, maxDistance);

            for (String text : sentences) {

                Chunking chunking = chunker.chunk(text);
                CharSequence cs = chunking.charSequence();
                Set<Chunk> chunkSet = chunking.chunkSet();

                for (Chunk chunk : chunkSet) {

                    CharSequence str = cs.subSequence(chunk.start(), chunk.end());

                    candidates.add(str.toString());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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

        StanfordCoreNLP pipeline = new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize, ssplit, pos, ner, parse",
                        "parse.model", "edu/stanford/nlp/models/pos-tagger/german/german-hgc.tagger",
                        "ner.model", "edu/stanford/nlp/models/ner/german.conll.hgc_175m_600.crf.ser.gz",
                        "ner.applyNumericClassifiers", "false",
                        "ner.useSUTime", "false",
                        "parse.model", "edu/stanford/nlp/models/lexparser/germanFactored.ser.gz",
                        "depparse.model", "edu/stanford/nlp/models/parser/nndep/UD_German.gz",
                        "depparse.language", "german",
                        "tokenize.language", "de"));

        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        List<String> candidates = new ArrayList<>();

        for (CoreMap sentence : sentences) {

            String lastNerTag = "";
            String organisationCandidate = "";

            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {

                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                if (ne.equals("I-ORG") && lastNerTag.equals("I-ORG")) {
                    organisationCandidate += word;
                } else if (ne.equals("I-ORG")) {
                    if (organisationCandidate != "")
                        candidates.add(organisationCandidate);

                    organisationCandidate = word;
                } else if (!ne.equals("I-ORG") && lastNerTag.equals("I-ORG")) {
                    candidates.add(organisationCandidate);
                }
            }

            if (!candidates.contains(organisationCandidate))
                candidates.add(organisationCandidate);
        }

        return candidates;
    }


    /**
     * Sentences of input text are detected and returned unchanged.
     * Detection is performed using OpenNLP sentence detection and the german model.
     *
     * @param text to analyse
     * @return String array containing sentences
     */
    private String[] detectSentences(String text) {

        try (InputStream modelIn = FileUtils.getFileInputStream("de-sent.bin")) {

            SentenceModel sentenceModel = new SentenceModel(modelIn);

            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentenceModel);

            return sentenceDetector.sentDetect(text);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Analyses the given sentence and returns it's part-of-speech structure
     *
     * @param sentence to analyse
     * @return array of pos-tags for each word of the sentence
     * @see STTS Stuttgart Tübingen tag set definition at http://www.datcatinfo.net/rest/dcs/376
     */
    private String[] analysePartOfSpeech(String sentence) {

        try (InputStream modelIn = FileUtils.getFileInputStream("de-pos-maxent.bin")) {

            POSModel posModel = new POSModel(modelIn);

            POSTaggerME tagger = new POSTaggerME(posModel);

            return tagger.tag(sentence.split(" "));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Cleans special characters form given string.
     *
     * @param word to clean
     * @return given word without any special character
     */
    private String clearWord(String word) {
        return word.replaceAll(SPECIAL_CHARS, "");
    }


    /**
     * Loads known company names form resources and returns them as
     * TrieDictionary - ready to use for the ApproxDictionaryChunker.
     *
     * @return dictionary of known company names
     */
    private TrieDictionary<String> getKnownOrganisations() {

        TrieDictionary<String> dictionary = new TrieDictionary<>();

        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.getFileInputStream("known_companies.txt"), "UTF8"));

            String line;
            while ((line = reader.readLine()) != null) {
                dictionary.addEntry(new DictionaryEntry<>(line, "Known-ORG"));
            }

            return dictionary;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return dictionary;
    }

}
