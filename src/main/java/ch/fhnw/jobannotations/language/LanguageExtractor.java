package ch.fhnw.jobannotations.language;

import ch.fhnw.jobannotations.Main;
import ch.fhnw.jobannotations.utils.ConfigurationUtil;
import ch.fhnw.jobannotations.utils.FileUtils;
import ch.fhnw.jobannotations.utils.PartOfSpeechUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;

public class LanguageExtractor {

    final static Logger LOG = Logger.getLogger(LanguageExtractor.class);

    // TODO: extract level of language
    public String parse(Document document) {

        if (Main.DEBUG) {
            System.out.println("\n" + StringUtils.repeat("-", 80));
            System.out.println("[language]\t" + "Started to parse language from offer");
        }

        // get the visible text from document
        final String text = document.body().text();

        // search for known languages
        /*Map<String, Integer> fuzzySearchCandidates = getFuzzySearchCandidates(text);

        // return comma separated list
        String result = "";

        for (Map.Entry<String, Integer> entry : fuzzySearchCandidates.entrySet()) {
            result += entry.getKey() + ", ";
        }

        if (result != "") {
            return result.substring(0, result.length() - 2);
        } else {
            LOG.debug("[language]\t" + "No languages found");
            return "";
        }*/

        return null;
    }

    private Map<String, String> getFuzzySearchCandidates(String text) {

        // get path to known languages model from configuration
        String knownLanguagesFile = ConfigurationUtil.getInstance().get("extractors.model.language");

        // load known languages from model as list
        List<String> languages = FileUtils.getFileContentAsList(knownLanguagesFile);

        // detect sentences to process
        String[] sentences = PartOfSpeechUtil.detectSentences(text);

        for (String language : languages) {

            for (String sentence : sentences) {

                if (sentence.toLowerCase().contains(language.toLowerCase())) {



                }

            }

        }
    }


}
