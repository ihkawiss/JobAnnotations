package ch.fhnw.jobannotations.language;

import ch.fhnw.jobannotations.utils.PartOfSpeechUtil;
import com.aliasi.dict.TrieDictionary;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;

public class LanguageExtractor {

    // TODO: extract level of language
    public String parse(Document document) {

        // get the visible text from document
        final String text = document.body().text();

        // search for known languages
        Map<String, Integer> fuzzySearchCandidates = getFuzzySearchCandidates(text);

        // return comma separated list
        String result = "";

        for (Map.Entry<String, Integer> entry : fuzzySearchCandidates.entrySet()) {
            result += entry.getKey() + ", ";
        }

        return result.substring(0, result.length() - 2);
    }

    private Map<String, Integer> getFuzzySearchCandidates(String text) {

        // get chunks for known organisation names which may be recognized within the text
        TrieDictionary knownCompanies = PartOfSpeechUtil.getTrieDictionaryByFile("data/known_languages.txt", "LANG");
        Map<String, Integer> foundChunks = PartOfSpeechUtil.getChunksByDictionary(knownCompanies, text, 1);

        // return found chunks as simple List<String>
        // TODO: use additional information such as score to enhance prediction
        Map<String, Integer> candidates = new HashMap<>();
        for (Map.Entry<String, Integer> entry : foundChunks.entrySet()) {

            String cleanedChunk = entry.getKey().replaceAll("[(),!.-]", "");

            if (!candidates.containsKey(cleanedChunk)) {
                candidates.put(cleanedChunk, entry.getValue());

                System.out.println("[language-approx]\t" + entry.getKey());
            }

        }

        return candidates;

    }


}