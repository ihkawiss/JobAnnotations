package ch.fhnw.jobannotations.language;

import ch.fhnw.jobannotations.utils.PartOfSpeechUtil;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.TrieDictionary;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LanguageExtractor {

    private TrieDictionary knownLanguages;

    public LanguageExtractor() {
        knownLanguages = PartOfSpeechUtil.getTrieDictionaryByFile("data/known_languages.txt", "LANG");
    }

    public String parse(Document document) {

        // get the visible text from document
        final String text = document.body().text();

        getFuzzySearchCandidates(text);

        return "";
    }

    private List<String> getFuzzySearchCandidates(String text) {

        Map<String, int[]> foundChunks = PartOfSpeechUtil.getChunksByDictionary(knownLanguages, text);

        // return found chunks as simple List<String>
        // TODO: use additional information such as score to enhance prediction
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : foundChunks.entrySet()) {
            candidates.add(entry.getKey());

            System.out.println("[lang-approx]\t" + PartOfSpeechUtil.clearWord(entry.getKey()));
        }
        // extract candidates
        return candidates;

    }


}
