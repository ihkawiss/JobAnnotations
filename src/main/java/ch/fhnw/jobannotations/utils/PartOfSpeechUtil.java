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
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PartOfSpeechUtil {

    // regex to filter special characters
    public static final String SPECIAL_CHARS = "[$&+,:;=?@#<>.^*%!-\"]";

    /**
     * Sentences of input text are detected and returned unchanged.
     * Detection is performed using OpenNLP sentence detection and the german model.
     *
     * @param text to analyse
     * @return String array containing sentences
     */
    public static String[] detectSentences(String text) {

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
     */
    public static String[] analysePartOfSpeech(String sentence) {

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
    public static String clearWord(String word) {
        return word.replaceAll(SPECIAL_CHARS, "");
    }


    public static Map<String, int[]> getChunksByDictionary(TrieDictionary<String> dictionary, String textToAnalyse) {

        Map<String, int[]> candidates = new HashMap<>();

        String[] sentences = detectSentences(textToAnalyse);

        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

        WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -1, -1, -1, Double.NaN);

        double maxDistance = 2;

        ApproxDictionaryChunker chunker = new ApproxDictionaryChunker(dictionary, tokenizerFactory, editDistance, maxDistance);

        for (String text : sentences) {

            Chunking chunking = chunker.chunk(text);
            CharSequence cs = chunking.charSequence();
            Set<Chunk> chunkSet = chunking.chunkSet();

            for (Chunk chunk : chunkSet) {

                // get start an end position of chunk
                int start = chunk.start();
                int end = chunk.end();
                double score = chunk.score();

                CharSequence str = cs.subSequence(chunk.start(), chunk.end());

                candidates.put(str.toString(), new int[]{start, end, (int) score});

            }
        }

        return candidates;

    }

    /**
     * Loads known company names form resources and returns them as
     * TrieDictionary - ready to use for the ApproxDictionaryChunker.
     *
     * @return dictionary of known company names
     */
    public static TrieDictionary<String> getTrieDictionaryByFile(String filename, String entityName) {

        TrieDictionary<String> dictionary = new TrieDictionary<>();

        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.getFileInputStream(filename), "UTF8"));

            String line;
            while ((line = reader.readLine()) != null) {
                dictionary.addEntry(new DictionaryEntry<>(line, entityName));
            }

            return dictionary;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return dictionary;
    }

}
