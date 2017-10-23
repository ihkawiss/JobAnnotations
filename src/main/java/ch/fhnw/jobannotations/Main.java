package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.jobtitle.JobTitleExtractor;
import ch.fhnw.jobannotations.language.LanguageExtractor;
import ch.fhnw.jobannotations.location.JobLocationExtractor;
import ch.fhnw.jobannotations.organisation.OrganisationExtractor;
import ch.fhnw.jobannotations.skills.JobSkillsExtractor;
import ch.fhnw.jobannotations.workload.JobWorkloadExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Hoang
 */
public class Main {

    // TODO: transfer to configuration file
    public static final boolean DEBUG = true;

    public static void main(String[] args) {
        boolean keepGoing = true;
        while (keepGoing) {
            boolean repeat = true;
            while (repeat) {
                System.out.print("Please enter a job offer (URL) to parse: ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                try {

                    if (DEBUG) {
                        System.out.println("NOTICE: DEBUG mode is enabled, extractors will print some messages.");
                    }

                    String url = br.readLine();
                    System.out.println("Sit tight, we're parsing the data...");

                    Document document = Jsoup.connect(url).get();

                    if (document == null) {
                        System.err.println("ERROR: Failed to load web page, please try again and report used URL!");

                    } else {
                        JobOffer jobOffer = new JobOffer(document);

                        // extract job title from offer
                        JobTitleExtractor jobTitleParser = new JobTitleExtractor();
                        String jobTitle = jobTitleParser.parseJobTitle(jobOffer);

                        // extract job location from offer
                        JobLocationExtractor jobLocationExtractor = new JobLocationExtractor();
                        String jobLocation = jobLocationExtractor.parseJobLocation(document);

                        // extract organisation from offer
                        OrganisationExtractor organisationExtractor = new OrganisationExtractor();
                        String jobOrganisation = organisationExtractor.parse(document);

                        // extract languages from offer
                        LanguageExtractor languageExtractor = new LanguageExtractor();
                        String jobLanguage = languageExtractor.parse(document);

                        // extract work load from offer
                        JobWorkloadExtractor workloadExtractor = new JobWorkloadExtractor();
                        String jobWorkload = workloadExtractor.parseJobWorkload(jobOffer);

                        // extract skills from offer
                        JobSkillsExtractor jobSkillsExtractor = new JobSkillsExtractor();
                        String jobSkills = jobSkillsExtractor.parseJobSkills(jobOffer);


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
