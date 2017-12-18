package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.domain.JobOffer;
import ch.fhnw.jobannotations.extractors.IExtractor;
import ch.fhnw.jobannotations.extractors.jobtitle.JobTitleExtractor;
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
 *
 *
 * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
 */
public class JobAnnotator {

    private final static Logger LOG = Logger.getLogger(JobAnnotator.class);

    private List<IExtractor> extractors;

    public JobAnnotator() {
        this(true);
    }

    /**
     * Constructs a JobAnnotator instance.
     *
     * @param useDefaultExtractors <code>false</code> to prevent using the default extractors list, <code>true</code>
     */
    public JobAnnotator(boolean useDefaultExtractors) {

        // load nlp models
        LOG.debug("Initializing NLP");
        NlpHelper.getInstance();

        extractors = new ArrayList<>();

        if (useDefaultExtractors) {
            extractors.add(new JobTitleExtractor());
            extractors.add(new JobSkillsExtractor());
            extractors.add(new LocationExtractor());
            extractors.add(new OrganisationExtractor());
            extractors.add(new LanguageExtractor());
            extractors.add(new JobWorkloadExtractor());
        }

    }

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

    public boolean add(IExtractor extractor) {
        return extractors.add(extractor);
    }

    public IExtractor remove(int index) {
        return extractors.remove(index);
    }

    public boolean remove(IExtractor extractor) {
        return extractors.remove(extractor);
    }


}
