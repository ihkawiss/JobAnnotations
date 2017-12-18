package ch.fhnw.jobannotations.extractors.organisation;

import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.utils.*;
import com.aliasi.dict.TrieDictionary;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible to identify potential organisation
 * names in a job offer document. To prevent false results and
 * to improve performance, various techniques are used.
 *
 * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
 */
public class OrganisationExtractor implements IExtractor {

    final static Logger LOG = Logger.getLogger(OrganisationExtractor.class);

    // official legal form postfixes in switzerland
    private static final String[] KNOWN_LEGAL_FORMS = {
            "AG", "Gen", "Genossenschaft", "GmbH", "KlG", "KmG", "KmAG", "SA", "SCoop", "Sàrl",
            "SNC", "SCm", "SCmA", "Sagl", "SAc", "SAcA", "Scrl", "SCl", "SACm"
    };

    /**
     * Extract texts from html node and it's children.
     *
     * @param parent which should be processed
     * @param texts  list where texts should be added
     */
    private void extractAllTagTexts(Element parent, List<String> texts) {
        texts.add(parent.ownText());
        for (Element element : parent.children()) {
            if (element != null) {
                extractAllTagTexts(element, texts);
            }
        }

    }

    /**
     * Identifies organisation candidates found in jobOffer.
     *
     * @param jobOffer to process
     */
    @Override
    public String parse(JobOffer jobOffer) {

        LOG.debug("Started to parse organisation from offer");

        // get the visible text from document
        final String text = jobOffer.getPlainText();

        // annotate sentences
        List<CoreMap> annotatedSentences = new ArrayList<>();
        for (String line : jobOffer.getPlainTextLines()) {
            annotatedSentences.addAll(NlpHelper.getInstance().getAnnotatedSentences(line));
        }

        // get text from all tags
        List<String> texts = new ArrayList<>();
        extractAllTagTexts(jobOffer.getDocument().body(), texts);


        // List<String> legalFormCandidates = getLegalFormCandidates(texts.toArray(new String[0]));
        List<String> legalFormCandidates = getLegalFormCandidates(annotatedSentences);
        Map<String, Integer> fuzzySearchCandidates = getFuzzySearchCandidates(text);

        // ---------------------------------------------------------
        // (1) analyse indicator results
        // ---------------------------------------------------------
        if (!legalFormCandidates.isEmpty()) {

            Map<String, Integer> weightedIndicatorCandidates = new HashMap<>();

            int posScore = 0;
            for (String candidate : legalFormCandidates) {

                if (StringUtils.isEmpty(candidate))
                    continue;

                // check against found fuzzy search chunks
                for (Map.Entry<String, Integer> fuzzyCandidate : fuzzySearchCandidates.entrySet()) {

                    if (candidate.toLowerCase().contains(fuzzyCandidate.getKey().toLowerCase())) {

                        if (weightedIndicatorCandidates.containsKey(candidate)) {

                            weightedIndicatorCandidates.put(candidate, weightedIndicatorCandidates.get(candidate) + 1 - fuzzyCandidate.getValue());

                        } else {
                            weightedIndicatorCandidates.put(candidate, 1 + posScore - fuzzyCandidate.getValue());
                        }

                    }

                }

                // no cross match found for candidate, rate at least with it's position score
                if (!weightedIndicatorCandidates.containsKey(candidate)) {
                    weightedIndicatorCandidates.put(candidate, posScore);
                }

                posScore++;
            }

            return weightedIndicatorCandidates.entrySet().stream().max((a, b) -> a.getValue() > b.getValue() ? 1 : -1).get().getKey().trim();

        }

        // ---------------------------------------------------------
        // (2) analyse fuzzy chunks for perfect matches
        // ---------------------------------------------------------
        long perfectMatches = fuzzySearchCandidates.entrySet().stream().filter(a -> a.getValue() == 0).count();

        if (perfectMatches > 0) {

            String lastMatch = "";

            for (Map.Entry<String, Integer> entry : fuzzySearchCandidates.entrySet()) {

                if (entry.getValue() == 0) {
                    lastMatch = entry.getKey(); // replacing means later position in document is higher rated
                }

            }

            return lastMatch.trim();
        }

        // ---------------------------------------------------------
        // (3) analyse fuzzy and ner for cross results
        // ---------------------------------------------------------
        LOG.debug("Need to look for NER - WARNING: this may take a while...");

        List<String> merged = new ArrayList<>();

        // add all fuzzy candidates keys
        fuzzySearchCandidates.entrySet().stream().forEach(t -> merged.add(t.getKey()));

        // add all ner tags found
        merged.addAll(getNerCandidates(text));

        // search for similarities
        Map<String, Integer> weighted = new HashMap<>();

        for (String candidate : merged) {

            for (String c : merged) {

                if (candidate.toLowerCase().contains(c.toLowerCase())) {

                    if (weighted.containsKey(candidate)) {

                        weighted.put(candidate, weighted.get(candidate) + 1);

                    } else {
                        weighted.put(candidate, 1);
                    }

                }

            }

        }

        if (weighted.entrySet().stream().max((a, b) -> a.getValue() > b.getValue() ? 1 : -1).isPresent()) {
            return weighted.entrySet().stream().max((a, b) -> a.getValue() > b.getValue() ? 1 : -1).get().getKey().trim();
        } else {
            return "organisation not found!";
        }
    }

    /**
     * Save found organisations into train file
     *
     * @param data found in job offer
     */
    @Override
    public void learn(String data) {
        FileUtils.addDataToTrainFile(ConfigurationUtil.get("extraction.organisations.train"), data);
    }

    /**
     * Within the passed sentence, a search for possible organisation names is performed.
     * If a probable organisation name is found, it's returned with a score from [0 - 100].
     *
     * @param annotatedSentences to analyse
     * @return probable organisation name with score
     */
    private List<String> getLegalFormCandidates(List<CoreMap> annotatedSentences) {

        List<String> candidates = new ArrayList<>();

        for (CoreMap annotatedSentence : annotatedSentences) {
            String sentence = annotatedSentence.get(CoreAnnotations.TextAnnotation.class);

            // search for known legal form postfixes in sentence
            for (String legalForm : KNOWN_LEGAL_FORMS) {
                if (sentence.toLowerCase().contains(legalForm.toLowerCase())) {

                    StringBuilder organisationNameBuilder = null;

                    List<CoreLabel> tokens = annotatedSentence.get(CoreAnnotations.TokensAnnotation.class);
                    for (int i = tokens.size() - 1; i >= 0; i--) {
                        CoreLabel token = tokens.get(i);
                        String word = token.get(CoreAnnotations.TextAnnotation.class);

                        if (StringUtils.simplify(word).equalsIgnoreCase(legalForm)) {
                            organisationNameBuilder = new StringBuilder();

                        } else if (organisationNameBuilder != null) {
                            String posTag = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                            boolean addToOrganisationName = (posTag.equals(NlpHelper.POS_TAG_COMMON_NOUN) || posTag.equals(NlpHelper.POS_TAG_PROPER_NOUN));
                            if (addToOrganisationName) {
                                // make sure word does not contain special chars
                                Pattern p = Pattern.compile(StringUtils.SPECIAL_CHARS);
                                Matcher m = p.matcher(word);
                                addToOrganisationName = !m.find();
                            }

                            if (addToOrganisationName) {
                                organisationNameBuilder.insert(0, word + " ");

                            } else {
                                String organisationName = organisationNameBuilder.toString().trim();
                                organisationNameBuilder = null;

                                getOrganisationName(candidates, legalForm, organisationName);
                            }
                        }
                    }
                    if (organisationNameBuilder != null) {
                        String organisationName = organisationNameBuilder.toString().trim();

                        getOrganisationName(candidates, legalForm, organisationName);
                    }
                }
            }
        }
        return candidates;
    }

    /**
     * Builds the potential company name based on input
     *
     * @param candidates
     * @param legalForm
     * @param organisationName
     */
    private void getOrganisationName(List<String> candidates, String legalForm, String organisationName) {
        if (organisationName.length() > legalForm.length()) {
            organisationName += " " + legalForm;
            if (!candidates.contains(organisationName)) {
                candidates.add(organisationName);

                LOG.debug("potential organisation found: " + organisationName);
            }
        }
    }

    /**
     * Analyses given text with LingPipe's basic NER class
     * ApproxDictionaryChunker, similar to a basic fuzzy search.
     *
     * @param text to analyse for organisation names
     * @return list of probable organisation names
     */
    private Map<String, Integer> getFuzzySearchCandidates(String text) {

        // get chunks for known organisation names which may be recognized within the text
        TrieDictionary knownCompanies = PartOfSpeechUtil.getTrieDictionaryByFile(ConfigurationUtil.get("extraction.organisations.train"), "ORG");
        Map<String, Integer> foundChunks = PartOfSpeechUtil.getChunksByDictionary(knownCompanies, text, 1);

        // return found chunks as simple List<String>
        Map<String, Integer> candidates = new HashMap<>();
        for (Map.Entry<String, Integer> entry : foundChunks.entrySet()) {
            candidates.put(entry.getKey(), entry.getValue());

            LOG.debug("potential organisation found: " + entry.getKey());
        }

        return candidates;
    }

    /**
     * Analyses given text with CoreNLP NER (named entity recognition)
     * and tries to identify probable organisation names.
     *
     * @param text to analyse for organisation names
     * @return list of probable organisation names
     */
    private List<String> getNerCandidates(String text) {

        StanfordCoreNLP pipeline = new StanfordCoreNLP(FileUtils.getStanfordCoreNLPGermanConfiguration());

        Annotation document = new Annotation(text);

        // run all Annotators on this text
        try {
            pipeline.annotate(document);
        } catch (Exception e) {
            LOG.error("Something went wrong while getting NER candidates!", e);
        }

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        List<String> candidates = new ArrayList<>();

        for (CoreMap sentence : sentences) {

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            String currentOrganisation = "";

            for (int i = 0; i < tokens.size(); i++) {

                CoreLabel token = tokens.get(i);

                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                if (ne.equals("I-ORG")) {
                    currentOrganisation = currentOrganisation + " " + word;
                } else if (currentOrganisation != "") {
                    candidates.add(currentOrganisation);

                    LOG.debug("potential organisation found: " + currentOrganisation);

                    currentOrganisation = "";
                }

            }

        }

        return candidates;
    }

}
