package ch.fhnw.jobannotations.extractors.language;

import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.JobOffer;
import ch.fhnw.jobannotations.utils.ConfigurationUtil;
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

public class LanguageExtractor implements IExtractor{

    @Override
    public String parse(JobOffer offer) {

        if (ConfigurationUtil.isDebugModeEnabled()) {
            System.out.println("\n" + StringUtils.repeat("-", 80));
            System.out.println("[language]\t" + "Started to parse language from offer");
        }

        // get the visible text from document
        List<String> lines = offer.getPlainTextLines();

        Map<String, String> fuzzySearchCandidates = getKnownLanguagesCandidates(lines);

        // return comma separated list
        String result = "";

        for (Map.Entry<String, String> entry : fuzzySearchCandidates.entrySet()) {
            result += entry.getKey();

            if(entry.getValue() != null && entry.getValue() != "")
                result += "(" + entry.getValue() + "), ";
            else
                result += ", ";
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
        List<String> languages = FileUtils.getFileContentAsList(ConfigurationUtil.get("extraction.languages.train"));

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

                    } else {

                        // fall back with trivial contains
                        if(sentence.toLowerCase().indexOf(language.toLowerCase()) > -1) {
                            if(candidates.get(language) == null)
                                candidates.put(language, "");
                        }

                    }

                }

            }

        }

        return candidates;
    }

}
