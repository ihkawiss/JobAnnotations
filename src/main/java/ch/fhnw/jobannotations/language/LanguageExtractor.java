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
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LanguageExtractor {

    final static Logger LOG = Logger.getLogger(LanguageExtractor.class);

    // TODO: extract level of language
    public String parse(JobOffer offer) {

        if (Main.DEBUG) {
            System.out.println("\n" + StringUtils.repeat("-", 80));
            System.out.println("[language]\t" + "Started to parse language from offer");
        }

        // get the visible text from document
        List<String> lines = offer.getPlainTextLines();

        Map<String, String> fuzzySearchCandidates = getFuzzySearchCandidates(lines);

        // return comma separated list
        String result = "";

        for (Map.Entry<String, String> entry : fuzzySearchCandidates.entrySet()) {
            result += entry.getKey() + "(" + entry.getValue() + "), ";
        }

        if (result != "") {
            return result.substring(0, result.length() - 2);
        } else {
            System.out.println("[language]\t" + "No languages found");
            return "";
        }
    }

    private Map<String, String> getFuzzySearchCandidates(List<String> lines) {

        Map<String, String> candidates = new HashMap<>();

        // load known languages from model as list
        List<String> languages = FileUtils.getFileContentAsList("data/known_languages.txt");

        for (String language : languages) {

            for (String sentence : lines) {

                if (sentence.toLowerCase().contains(language.toLowerCase())) {

                    // annotate sentence
                    CoreMap annotatedSentence = NlpHelper.getInstance().getAnnotatedSentences(sentence).get(0);

                    SemanticGraph dependencies = annotatedSentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);

                    // find word, which contains language
                    IndexedWord root = dependencies.getNodeByWordPattern("(?i)" + language + ".*");

                    if (root != null && root.word().toLowerCase().contains(language.toLowerCase())) {
                        Set<IndexedWord> desc = dependencies.descendants(root);
                        List<IndexedWord> predicates = desc.stream().filter(d -> d.tag().equals("ADV") || d.tag().equals("ADJA")).collect(Collectors.toList());

                        String a = "";
                        for (IndexedWord w : predicates)
                            a += w.lemma() + " ";

                        candidates.put(language, a);
                    }

                }

            }

        }

        return candidates;
    }


}
