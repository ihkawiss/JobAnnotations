package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.jobtitle.JobTitleExtractor;
import ch.fhnw.jobannotations.location.JobLocationExtractor;
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

    public static void main(String[] args) {
        boolean keepGoing = true;
        while (keepGoing) {
            boolean repeat = true;
            while (repeat) {
                System.out.print("Enter URL: ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                try {
                    String url = br.readLine();
                    System.out.println("Parsing Data...");

                    Document document = Jsoup.connect(url).get();

                    if (document == null) {
                        System.err.println("Failed to load web page");

                    } else {
                        JobOffer jobOffer = new JobOffer(document);

                        JobTitleExtractor jobTitleParser = new JobTitleExtractor();
                        String jobTitle = jobTitleParser.parseJobTitle(jobOffer);

                        JobLocationExtractor jobLocationExtractor = new JobLocationExtractor();
                        String location = jobLocationExtractor.parseJobLocation(document);

                        /*
                        OrganisationExtractor organisationExtractor = new OrganisationExtractor();
                        String organisation = organisationExtractor.parse(document);
                        System.out.println("Firma: " + organisation);
                        */

                        JobWorkloadExtractor jobWorkloadExtractor = new JobWorkloadExtractor();
                        String workload = jobWorkloadExtractor.parseJobWorkload(jobOffer);

                        JobSkillsExtractor jobSkillsExtractor = new JobSkillsExtractor();
                        String skills = jobSkillsExtractor.parseJobSkills(jobOffer);

                        System.out.println("Titel: " + jobTitle);
                        System.out.println("Arbeitsort: " + location);
                        System.out.println("Pensum: " + workload);
                        System.out.println("Anforderungen:\n" + skills);

                    }
                } catch (IOException e) {
                    System.out.println("Failed to read URL");
                }
                System.out.println("");
                repeat = false;
            }
        }
        //String url = "https://primework.ch/de/d/backend-web-developer-php-100-/24454";

        /*
        List<String> jobs = new ArrayList<>();
        jobs.add("Web-Developer");
        jobs.add("Web Entwickler");
        jobs.add("Engineer");
        jobs.add("Developer");
        jobs.add("Entwickler");
        ExtractedResult extractedResult = FuzzySearch.extractOne("Wir suchen einen PHP Web Entwickelr für unser Team bei unserem neuen Standort in Bern.", jobs);
        System.out.println(extractedResult.toString());*/
    }
}
