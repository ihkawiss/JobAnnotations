package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.jobtitle.JobTitleExtractor;
import ch.fhnw.jobannotations.language.LanguageExtractor;
import ch.fhnw.jobannotations.location.LocationExtractor;
import ch.fhnw.jobannotations.organisation.OrganisationExtractor;
import ch.fhnw.jobannotations.skills.JobSkillsExtractor;
import ch.fhnw.jobannotations.utils.NlpHelper;
import ch.fhnw.jobannotations.workload.JobWorkloadExtractor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;

/**
 * @author Hoang
 */
public class Main {

    // TODO: transfer to configuration file
    public static final boolean DEBUG = true;

    public static void main(String[] args) {
        boolean keepGoing = true;

        // print welcome message
        System.out.println(StringUtils.repeat("-", 80));
        System.out.println("Welcome to JobAnnotations!\n");
        System.out.println("Project:\tJobAnnotations FHNW IP5 2017");
        System.out.println("Version:\tBeta");
        System.out.println("Authors:\tHoang Tran, Kevin Kirn");
        System.out.println(StringUtils.repeat("-", 80));

        // init nlp helper
        NlpHelper.getInstance();

        if (DEBUG) {
            System.out.println(StringUtils.repeat("-", 80));
            System.out.println("NOTICE: DEBUG mode is enabled, extractors will print some messages.");
        }

        while (keepGoing) {

            boolean repeat = true;

            while (repeat) {
                System.out.println(StringUtils.repeat("-", 80)+"\n");
                System.out.print("Please enter a job offer (URL) to parse: ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                try {

                    String url = br.readLine();
                    System.out.println("\nSit tight, we're parsing the data...");

                    if (StringUtils.isEmpty(url)) {
                        System.err.println("Entered URL is empty. Please try again.");
                        continue;
                    }

                    url = URLDecoder.decode(url);

                    Document document = Jsoup.connect(url).get();

                    if (document == null) {
                        System.err.println("\nERROR: Failed to load web page, please try again and report used URL!");

                    } else {
                        JobOffer jobOffer = new JobOffer(document);

                        /// extract job title from offer
                        JobTitleExtractor jobTitleParser = new JobTitleExtractor();
                        String jobTitle = jobTitleParser.parseJobTitle(jobOffer);

                        // extract skills from offer
                        JobSkillsExtractor jobSkillsExtractor = new JobSkillsExtractor();
                        String jobSkills = jobSkillsExtractor.parseJobSkills(jobOffer);

                        // extract job location from offer
                        LocationExtractor locationExtractor = new LocationExtractor();
                        String jobLocation = locationExtractor.parse(jobOffer);

                        // extract organisation from offer
                        OrganisationExtractor organisationExtractor = new OrganisationExtractor();
                        String jobOrganisation = organisationExtractor.parse(jobOffer);

                        // clean company from jobtitle
                        jobTitle = OrganisationExtractor.removeOrganisationFromString(jobOrganisation, jobTitle);

                        // extract languages from offer
                        LanguageExtractor languageExtractor = new LanguageExtractor();
                        String jobLanguage = languageExtractor.parse(jobOffer);

                        // extract work load from offer
                        JobWorkloadExtractor workloadExtractor = new JobWorkloadExtractor();
                        String jobWorkload = workloadExtractor.parseJobWorkload(jobOffer);

                        // PRINT REPORT OF FOUND RESULTS
                        System.out.println("\n" + StringUtils.repeat("-", 80));
                        System.out.println("RESULT REPORT");
                        System.out.println(StringUtils.repeat("-", 80));

                        System.out.println("Job title:\t\t" + jobTitle);
                        System.out.println("Quota:\t\t\t" + jobWorkload);
                        System.out.println("Company:\t\t" + jobOrganisation);
                        System.out.println("Languages:\t\t" + jobLanguage);
                        System.out.println("Location:\t\t" + jobLocation);
                        System.out.println("Skills:\t\n" + jobSkills);


                    }
                } catch (IOException e) {
                    System.out.println("Failed to read URL");
                }
                System.out.println("");
                repeat = false;
            }
        }

    }
}
