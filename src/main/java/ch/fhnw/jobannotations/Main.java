package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.jobtitle.JobTitleExtractor;
import ch.fhnw.jobannotations.language.LanguageExtractor;
import ch.fhnw.jobannotations.location.JobLocationExtractor;
import ch.fhnw.jobannotations.organisation.OrganisationExtractor;
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
                        JobTitleExtractor jobTitleParser = new JobTitleExtractor();
                        String jobTitle = jobTitleParser.parseJobTitle(document);

                        JobLocationExtractor jobLocationExtractor = new JobLocationExtractor();
                        //jobLocationExtractor.parseJobLocation(document);

                        OrganisationExtractor organisationExtractor = new OrganisationExtractor();
                        String organisation = organisationExtractor.parse(document);

                        LanguageExtractor languageExtractor = new LanguageExtractor();
                        String language = languageExtractor.parse(document);

                        System.out.println("ORG BEST MATCH: " + organisation);
                        System.out.println("LANG BEST MATCH: " + language);

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
