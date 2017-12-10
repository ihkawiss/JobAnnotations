package ch.fhnw.jobannotations.language;

import ch.fhnw.jobannotations.JobOffer;
import ch.fhnw.jobannotations.Main;
import ch.fhnw.jobannotations.utils.FileUtils;
import ch.fhnw.jobannotations.utils.NlpHelper;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LanguageExtractor {

    public String parse(JobOffer offer) {

        if (Main.DEBUG) {
            System.out.println("\n" + StringUtils.repeat("-", 80));
            System.out.println("[language]\t" + "Started to parse language from offer");
        }

        // get the visible text from document
        List<String> lines = offer.getPlainTextLines();

        Map<String, String> fuzzySearchCandidates = getKnownLanguagesCandidates(lines);

        // return comma separated list
        String result = "";

        for (Map.Entry<String, String> entry : fuzzySearchCandidates.entrySet()) {
            result += entry.getKey() + " (" + entry.getValue() + "), ";
        }

        if (result != "") {
            return result.substring(0, result.length() - 2);
        } else {
            System.out.println("[language]\t" + "No languages found");
            return "";
        }
    }

    /**
     * Find known languages in job offer and determine it's levels.
     *
     * @param lines of the job offer
     * @return candidates which were found
     */
    private Map<String, String> getKnownLanguagesCandidates(List<String> lines) {

        Map<String, String> candidates = new HashMap<>();

        // load known languages from model as list
        List<String> languages = FileUtils.getFileContentAsList("data/known_languages.txt");

        for (String language : languages) {

            for (String sentence : lines) {

                if (sentence.toLowerCase().contains(language.toLowerCase())) {

                    // annotate sentence
                    CoreMap annotatedSentence = NlpHelper.getInstance().getAnnotatedSentences(sentence).get(0);

                    // load dependency graph from annotated sentence
                    SemanticGraph dependencies = annotatedSentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);

                    // find word, which contains language
                    IndexedWord languageWord = dependencies.getNodeByWordPattern("(?i)" + language + ".*");

                    if (languageWord != null) {

                        // find all words which are descendants of the language
                        Set<IndexedWord> desc = dependencies.descendants(languageWord);

                        // extract adjectives from dependencies
                        List<String> predicates = desc.stream()
                                .filter(d -> d.tag().equals("ADV") || d.tag().equals("ADJA") || d.tag().equals("ADJD"))
                                .map(x -> x.word())
                                .collect(Collectors.toList());

                        // join found adjectives together
                        String adjectives = String.join(" ", predicates);

                        // save found language and adjectives
                        candidates.put(language, adjectives);
                    }

                }

            }

        }

        return candidates;
    }

}
