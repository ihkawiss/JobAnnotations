package ch.fhnw.jobannotations.utils;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.ApproxDictionaryChunker;
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
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Singleton class for NLP related operations. This class is responsible for initializing the NLP models. Provides utility
 * methods for NLP operations.
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class NlpHelper {
    private final static Logger LOG = Logger.getLogger(NlpHelper.class);

    public static final int DICTIONARY_DISTANCE_MIN_VALUE = 1000;
    public static final String NER_TAG_LOCATION = "I-LOC";
    public static final String POS_TAG_COMMON_NOUN = "NN";
    public static final String POS_TAG_PROPER_NOUN = "NE"; // includes names, cities, etc

    private static NlpHelper instance;
    private StanfordCoreNLP pipeline;
    private final TrieDictionary<String> titlesDictionary;
    private final TrieDictionary<String> skillsDictionary;
    private final TrieDictionary<String> antiSkillsDictionary;
    private final TrieDictionary<String> locationsDictionary;

    private NlpHelper() {
        LOG.debug("Initializing NLP");
        pipeline = new StanfordCoreNLP(FileUtils.getStanfordCoreNLPGermanConfiguration());

        LOG.debug("Loading dictionaries");
        titlesDictionary = PartOfSpeechUtil.getTrieDictionaryByFile(ConfigurationUtil.get("extraction.titles.train"), "TITLE");
        skillsDictionary = PartOfSpeechUtil.getTrieDictionaryByFile(ConfigurationUtil.get("extraction.skills.train.positive"), "SKILL");
        antiSkillsDictionary = PartOfSpeechUtil.getTrieDictionaryByFile(ConfigurationUtil.get("extraction.skills.train.negative"), "ANTISKILL");
        locationsDictionary = PartOfSpeechUtil.getTrieDictionaryByFile(ConfigurationUtil.get("extraction.locations.train"), "LOCATION");
    }

    /**
     * Returns the static instance of this class. Creates a new instance if it is currently null.
     *
     * @return Static instance of this class
     */
    public static NlpHelper getInstance() {
        if (instance == null) {
            instance = new NlpHelper();
        }
        return instance;
    }

    /**
     * Annotates the sentences of the given text and returns them.
     *
     * @param text Text to be annotated
     * @return Annotated sentences
     */
    public List<CoreMap> getAnnotatedSentences(String text) {
        Annotation document = new Annotation(text);

        pipeline.annotate(document);
        return document.get(CoreAnnotations.SentencesAnnotation.class);
    }

    /**
     * Extracts nouns of all sentences in given List and adds them to a single List.
     *
     * @param sentences List of sentences to extract nouns from
     * @return List of extracted nouns
     */
    public List<String> extractNouns(List<String> sentences) {
        List<String> nouns = new ArrayList<>();
        for (String sentence : sentences) {
            nouns.addAll(extractNouns(sentence));
        }

        return nouns;
    }

    /**
     * Extracts nouns of given sentence.
     *
     * @param sentence Sentence to extract nouns from
     * @return List of extracted nouns
     */
    public List<String> extractNouns(String sentence) {
        List<String> nouns = new ArrayList<>();
        List<CoreMap> annotatedSentences = getAnnotatedSentences(sentence);
        for (CoreMap annotatedSentence : annotatedSentences) {
            List<CoreLabel> tokens = annotatedSentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                String posTag = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                if (POS_TAG_COMMON_NOUN.equals(posTag) || POS_TAG_PROPER_NOUN.equals(posTag)) {
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    nouns.add(word);
                }
            }

        }
        return nouns;
    }

    /**
     * Calculates distance of given word with entries in given dictionary with given max distance.
     *
     * @param dictionary Dictionary to be used to calculate distance
     * @param word Word to be used to calculate distance
     * @param maxDistance Max distance to be used
     * @return IntStringPair of calculated distance and analyzed word
     */
    public IntStringPair calcDistanceWithDictionary(TrieDictionary<String> dictionary, String word, double maxDistance) {

        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -1, -1, -1, Double.NaN);
        ApproxDictionaryChunker chunker = new ApproxDictionaryChunker(dictionary, tokenizerFactory, editDistance, maxDistance);

        Chunking chunking = chunker.chunk(word);
        CharSequence charSequence = chunking.charSequence();
        Set<Chunk> chunkSet = chunking.chunkSet();
        double wordLength = charSequence.length();

        IntStringPair bestMatch = null;

        for (Chunk chunk : chunkSet) {
            double distance = chunk.score();
            int start = chunk.start();
            int end = chunk.end();
            String analyzedWord = charSequence.subSequence(start, end).toString();
            int analyzedLength = analyzedWord.length();

            // calculate distance ratio
            int ratio = (int) (DICTIONARY_DISTANCE_MIN_VALUE * (wordLength / analyzedLength + distance / analyzedLength));

            if (bestMatch != null) {

                if (ratio > bestMatch.getInt()) {
                    // skip words with higher ratio
                    continue;
                }

                if (ratio == bestMatch.getInt() && analyzedLength < bestMatch.getString().length()) {
                    // skip shorter words with same ratio
                    continue;
                }
            }

            // keep better word
            bestMatch = new IntStringPair(ratio, analyzedWord);
        }

        return bestMatch;
    }

    public TrieDictionary<String> getTitlesDictionary() {
        return titlesDictionary;
    }

    public TrieDictionary<String> getSkillsDictionary() {
        return skillsDictionary;
    }

    public TrieDictionary<String> getAntiSkillsDictionary() {
        return antiSkillsDictionary;
    }

    public TrieDictionary<String> getLocationsDictionary() {
        return locationsDictionary;
    }
}
