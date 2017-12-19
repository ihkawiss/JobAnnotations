package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.extractors.jobtitle.TitleExtractor;
import ch.fhnw.jobannotations.extractors.language.LanguageExtractor;
import ch.fhnw.jobannotations.extractors.location.LocationExtractor;
import ch.fhnw.jobannotations.extractors.organisation.OrganisationExtractor;
import ch.fhnw.jobannotations.extractors.skills.JobSkillsExtractor;
import ch.fhnw.jobannotations.extractors.workload.JobWorkloadExtractor;
import ch.fhnw.jobannotations.utils.NlpHelper;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Parses job offer information from job offer documents by using extractors that implement the {@link IExtractor}
 * interface.
 *
 * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
 */
public class JobAnnotator {

    private final static Logger LOG = Logger.getLogger(JobAnnotator.class);

    private List<IExtractor> extractors;

    /**
     * Constructs a JobAnnotator instance with default extractor List.
     */
    public JobAnnotator() {
        this(true);
    }

    /**
     * Constructs a JobAnnotator instance.
     *
     * @param useDefaultExtractors <code>false</code> to prevent using the default extractors list, <code>true</code> otherwise
     */
    public JobAnnotator(boolean useDefaultExtractors) {

        // load nlp models
        LOG.debug("Initializing NLP");
        NlpHelper.getInstance();

        extractors = new ArrayList<>();

        if (useDefaultExtractors) {
            LOG.debug("Using default extractors");
            extractors.add(new TitleExtractor());
            extractors.add(new JobSkillsExtractor());
            extractors.add(new LocationExtractor());
            extractors.add(new OrganisationExtractor());
            extractors.add(new LanguageExtractor());
            extractors.add(new JobWorkloadExtractor());
        }

    }

    /**
     * Parses job offer information from given url by using the added extractors and returns the results as a map with
     * extractor class name as key and parsed results as value. An empty map may be returned if parsing fails.
     *
     * @param url Url of job offer document to be parsed
     * @return Parsed job offer information as a map or an empty map if something failed
     */
    public HashMap<String, String> parse(String url) {
        HashMap<String, String> result = new HashMap<>();

        try {
            // create job offer by url
            Document document = Jsoup.connect(url).get();
            JobOffer jobOffer = new JobOffer(document);

            for (IExtractor extractor : extractors) {

                String candidates = extractor.parse(jobOffer);

                extractor.learn(candidates);

                result.put(extractor.getClass().getSimpleName(), candidates);

            }

        } catch (IOException e) {
            LOG.error("Something went wrong while parsing the job offer:\n", e);
        }

        return result;
    }

    /**
     * Adds extractor to extractor List.
     *
     * @param extractor Extractor to be added
     * @return <code>true</code> if extractor added, <code>false</code> otherwise
     * @see List#add(Object)
     */
    public boolean add(IExtractor extractor) {
        return extractors.add(extractor);
    }

    /**
     * Removes extractor from List with given index.
     *
     * @param index Index of extractor to be removed
     * @return Extractor that has been removed
     * @see List#remove(int)
     */
    public IExtractor remove(int index) {
        return extractors.remove(index);
    }

    /**
     * Removes given extractor from List.
     *
     * @param extractor Extractor to be removed
     * @return <code>true</code> if extractor removed, <code>false</code> otherwise
     * @see List#remove(Object)
     */
    public boolean remove(IExtractor extractor) {
        return extractors.remove(extractor);
    }

    /**
     * Checks whether the given extractor is present in the List or not.
     *
     * @param extractor Extractor to check
     * @return <code>true</code> if extractor present, <code>false</code> otherwise
     * @see List#contains(Object)
     */
    public boolean contains(IExtractor extractor) {
        return extractors.contains(extractor);
    }
}
