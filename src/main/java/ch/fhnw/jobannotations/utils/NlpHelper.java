package ch.fhnw.jobannotations.utils;

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
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author Hoang
 */

// TODO rename?
public class NlpHelper {
    public static final String NER_TAG_LOCATION = "I-LOC";
    public static final String POS_TAG_COMMON_NOUN = "NN";
    public static final String POS_TAG_PROPER_NOUN = "NE"; // includes names, cities, etc

    private static NlpHelper instance;
    private StanfordCoreNLP pipeline;
    private MaxentTagger tagger;
    private DependencyParser parser;
    private final TrieDictionary<String> skillsDictionary;
    private final TrieDictionary<String> simplifiedSkillsDictionary;
    private final TrieDictionary<String> antiSkillsDictionary;
    private final TrieDictionary<String> simplifiedAntiSkillsDictionary;

    private NlpHelper() {
        System.out.println("[nlp]\tInitializing NLP");
        pipeline = new StanfordCoreNLP(FileUtils.getStanfordCoreNLPGermanConfiguration());

        System.out.println("[nlp]\tLoading dictionaries");
        skillsDictionary = PartOfSpeechUtil.getTrieDictionaryByFile(ConfigurationUtil.get("extraction.skills.train.positive"), "SKILL");
        antiSkillsDictionary = PartOfSpeechUtil.getTrieDictionaryByFile(ConfigurationUtil.get("extraction.skills.train.negative"), "ANTISKILL");

        // create dictionaries with cleaned entries (lower case without special chars)
        simplifiedSkillsDictionary = createSimplifiedDictionary(skillsDictionary);
        simplifiedAntiSkillsDictionary = createSimplifiedDictionary(antiSkillsDictionary);
    }

    public static NlpHelper getInstance() {
        if (instance == null) {
            instance = new NlpHelper();
        }
        return instance;
    }

    public StanfordCoreNLP getPipeline() {
        return pipeline;
    }

    public List<GrammaticalStructure> getGrammaticalStructures(String text) {
        List<GrammaticalStructure> grammaticalStructures = new ArrayList<>();
        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
        for (List<HasWord> sentence : tokenizer) {
            List<TaggedWord> tagged = tagger.tagSentence(sentence);
            grammaticalStructures.add(parser.predict(tagged));
        }
        return grammaticalStructures;
    }

    public List<List<HasWord>> getTaggedSentences(String text) {
        List<List<HasWord>> taggedSentences = new ArrayList<>();
        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
        for (List<HasWord> sentence : tokenizer) {
            taggedSentences.add(sentence);
        }
        return taggedSentences;
    }

    public SemanticGraph getSemanticGraphOfSentence(CoreMap annotatedSentence) {
        return annotatedSentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);


        //((ArrayList<TypedDependency>)annotatedSentences.get(0).get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).typedDependencies()).get(1).dep().tag()

    }

    public List<CoreLabel> getTokensOfSentence(CoreMap annotatedSentence) {
        // document.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(CoreAnnotations.TokensAnnotation.class).get(10).get(CoreAnnotations.NamedEntityTagAnnotation.class)
        return annotatedSentence.get(CoreAnnotations.TokensAnnotation.class);
    }

    public List<CoreMap> getAnnotatedSentences(String text) {
        Annotation document = new Annotation(text);

        pipeline.annotate(document);
        return document.get(CoreAnnotations.SentencesAnnotation.class);
    }

    public List<String> extractNouns(List<String> sentences) {
        List<String> nouns = new ArrayList<>();
        for (String sentence : sentences) {
            nouns.addAll(extractNouns(sentence));
        }

        return nouns;
    }

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
            int ratio = (int) (1000 * (wordLength / analyzedLength - distance / analyzedLength));

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

    private Properties getDependencyParserProperties() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.getResourceInputStream("configuration/StanfordCoreNLP-german-dependency-parser.properties"), "UTF8"));

            Properties props = new Properties();
            props.load(reader);

            return props;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private TrieDictionary<String> createSimplifiedDictionary(TrieDictionary<String> dictionary) {
        TrieDictionary<String> simplifiedDictionary = new TrieDictionary<>();
        for (DictionaryEntry<String> entry : dictionary) {
            String simplifiedPhrase = StringUtils.simplify(entry.phrase());
            DictionaryEntry<String> simplifiedEntry = new DictionaryEntry<>(simplifiedPhrase, entry.category());
            simplifiedDictionary.addEntry(simplifiedEntry);
        }
        return simplifiedDictionary;
    }


    public TrieDictionary<String> getSkillsDictionary() {
        return skillsDictionary;
    }

    public TrieDictionary<String> getAntiSkillsDictionary() {
        return antiSkillsDictionary;
    }

    public TrieDictionary<String> getSimplifiedSkillsDictionary() {
        return simplifiedSkillsDictionary;
    }

    public TrieDictionary<String> getSimplifiedAntiSkillsDictionary() {
        return simplifiedAntiSkillsDictionary;
    }
}
