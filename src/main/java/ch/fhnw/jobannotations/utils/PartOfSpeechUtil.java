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

    /**
     * Sentences of input text are detected and returned unchanged.
     * Detection is performed using OpenNLP sentence detection and the german model.
     *
     * @param text to analyse
     * @return String array containing sentences
     */
    public static String[] detectSentences(String text) {

        try (InputStream modelIn = FileUtils.getFileAsInputStream(ConfigurationUtil.get("external.OpenNLP.models.sentence"))) {

            SentenceModel sentenceModel = new SentenceModel(modelIn);

            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentenceModel);

            return sentenceDetector.sentDetect(text);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Map<String, Integer> getChunksByDictionary(TrieDictionary<String> dictionary, String textToAnalyse, double distance) {
        Map<String, Integer> candidates = new HashMap<>();

        String[] sentences = detectSentences(textToAnalyse);
        if (sentences == null) {
            return candidates;
        }

        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -1, -1, -1, Double.NaN);
        ApproxDictionaryChunker chunker = new ApproxDictionaryChunker(dictionary, tokenizerFactory, editDistance, distance);

        for (String text : sentences) {

            Chunking chunking = chunker.chunk(text);
            CharSequence cs = chunking.charSequence();
            Set<Chunk> chunkSet = chunking.chunkSet();

            for (Chunk chunk : chunkSet) {
                int start = chunk.start();
                int end = chunk.end();
                int score = (int) chunk.score();
                CharSequence str = cs.subSequence(start, end);
                candidates.put(str.toString(), score);

            }
        }

        return candidates;

    }

    /**
     * Loads known words form resources and returns them as
     * TrieDictionary - ready to use for the ApproxDictionaryChunker.
     *
     * @return dictionary of known words
     */
    public static TrieDictionary<String> getTrieDictionaryByFile(String filename, String entityName) {

        TrieDictionary<String> dictionary = new TrieDictionary<>();

        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.getFileAsInputStream(filename), "UTF8"));

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
