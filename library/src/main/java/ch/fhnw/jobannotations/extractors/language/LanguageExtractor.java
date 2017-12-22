package ch.fhnw.jobannotations.extractors.language;

import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.utils.ConfigurationUtil;
import ch.fhnw.jobannotations.utils.FileUtils;
import ch.fhnw.jobannotations.utils.NlpHelper;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is responsible to identify potential languages in a job offer document. To prevent false results and to
 * improve performance, various techniques are used.
 *
 * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
 */
public class LanguageExtractor implements IExtractor {

    private final static Logger LOG = Logger.getLogger(LanguageExtractor.class);

    /**
     * Identifies language candidates found in jobOffer.
     *
     * @param jobOffer to process
     */
    @Override
    public String parse(JobOffer jobOffer) {

        LOG.debug("Started to parse language from offer");

        // get the visible text from document
        List<String> lines = jobOffer.getPlainTextLines();

        Map<String, String> fuzzySearchCandidates = getKnownLanguagesCandidates(lines);

        // return comma separated list
        String result = "";

        for (Map.Entry<String, String> entry : fuzzySearchCandidates.entrySet()) {
            result += entry.getKey();

            if (entry.getValue() != null && entry.getValue() != "")
                result += "(" + entry.getValue() + "), ";
            else
                result += ", ";
        }

        if (result != "") {
            return result.substring(0, result.length() - 2);
        } else {
            LOG.debug("No languages found");

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
                        if (sentence.toLowerCase().indexOf(language.toLowerCase()) > -1) {
                            if (candidates.get(language) == null)
                                candidates.put(language, "");
                        }

                    }

                }

            }

        }

        return candidates;
    }

    @Override
    public void learn(String data) {
        // NOP - makes currently no sense
    }

}
