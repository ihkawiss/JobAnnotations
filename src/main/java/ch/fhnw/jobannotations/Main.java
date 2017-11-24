package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.jobtitle.TitleExtractor;
import ch.fhnw.jobannotations.location.LocationExtractor;
import ch.fhnw.jobannotations.skills.JobSkillsExtractor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Hoang
 */
public class Main {

    // TODO: transfer to configuration file
    public static final boolean DEBUG = true;

    public static void main(String[] args) {

        BufferedReader brPath = new BufferedReader(new InputStreamReader(System.in));
        String path;
        try {
            System.out.print("Enter path of input file: ");
            path = brPath.readLine();
        } catch (IOException e) {
            System.err.println("Failed to read file");
            e.printStackTrace();
            return;
        } finally {
            try {
                brPath.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (path == null) {
            System.out.println("Failed to get file path");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            Set<String> skillSet = new HashSet<>();

            String url = br.readLine();
            while (url != null && !url.trim().isEmpty()) {
                try {
                    System.out.println("Parsing: " + url);
                    url = URLDecoder.decode(url);

                    Document document = Jsoup.connect(url).get();
                    if (document == null) {
                        System.err.println("\nERROR: Failed to load web page, please try again and report used URL!");
                        continue;
                    }

                    if (url.contains("www.jobs.ch")) {
                        Elements iframes = document.getElementsByTag("iframe");
                        if (iframes.size() > 1) {
                            String directLink = Jsoup.parse((iframes.get(1)).attr("srcdoc")).getElementsByTag("a").attr("href");

                            System.out.println("Parsing direct link: " + directLink);
                            url = URLDecoder.decode(directLink);

                            document = Jsoup.connect(url).get();
                        }
                    }


                    if (document == null) {
                        System.err.println("\nERROR: Failed to load web page, please try again and report used URL!");

                    } else {
                        JobOffer jobOffer = new JobOffer(document);

                        // extract skills from offer
                        JobSkillsExtractor jobSkillsExtractor = new JobSkillsExtractor();
                        String jobSkills = jobSkillsExtractor.parseJobSkills(jobOffer);
                        for (String skill : jobSkills.split("\n")) {
                            skill = skill.trim();
                            if (!skill.isEmpty() && !skillSet.contains(skill)) {
                                skillSet.add(skill);
                                System.out.println("\t" + skill);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: Exception thrown: " + e.toString());
                }

                url = br.readLine();
            }

            System.out.println();
            System.out.println("FOUND SKILLS:");
            for (String skill : skillSet) {
                System.out.println(skill);
            }


        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to read file");
        }
    }

    public static void mainBackup(String[] args) {
        boolean keepGoing = true;

        // print welcome message
        System.out.println(StringUtils.repeat("-", 80));
        System.out.println("Welcome to JobAnnotations!\n");
        System.out.println("Project:\tJobAnnotations FHNW IP5 2017");
        System.out.println("Version:\tBeta");
        System.out.println("Authors:\tHoang Tran, Kevin Kirn");
        System.out.println(StringUtils.repeat("-", 80));

        if (DEBUG) {
            System.out.println("NOTICE: DEBUG mode is enabled, extractors will print some messages.\n");
        }

        while (keepGoing) {

            boolean repeat = true;

            while (repeat) {

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

                        /*
                        // extract job title from offer
                        JobTitleExtractor jobTitleParser = new JobTitleExtractor();
                        String jobTitle = jobTitleParser.parseJobTitle(jobOffer);
*/

                        // extract skills from offer
                        JobSkillsExtractor jobSkillsExtractor = new JobSkillsExtractor();
                        String jobSkills = jobSkillsExtractor.parseJobSkills(jobOffer);

                        // extract job location from offer
                        LocationExtractor locationExtractor = new LocationExtractor();
                        String jobLocation = locationExtractor.parse(jobOffer);

                        /*
                        // extract organisation from offer
                        // TODO: use found title to enhance extraction since the company name may be already present there (often in <title> tag)
                        OrganisationExtractor organisationExtractor = new OrganisationExtractor();
                        String jobOrganisation = organisationExtractor.parse(document);

                        // clean company from jobtitle
                        jobTitle = OrganisationExtractor.removeOrganisationFromString(jobOrganisation, jobTitle);

                        // extract languages from offer
                        LanguageExtractor languageExtractor = new LanguageExtractor();
                        String jobLanguage = languageExtractor.parse(document);

                        // extract work load from offer
                        JobWorkloadExtractor workloadExtractor = new JobWorkloadExtractor();
                        String jobWorkload = workloadExtractor.parseJobWorkload(jobOffer);
                        */


                        TitleExtractor titleExtractor = new TitleExtractor();
                        System.out.println(titleExtractor.parse(jobOffer));

                        // PRINT REPORT OF FOUND RESULTS
                        System.out.println("\n" + StringUtils.repeat("-", 80));
                        System.out.println("RESULT REPORT");
                        System.out.println(StringUtils.repeat("-", 80));
                        /*
                        System.out.println("Job title:\t\t" + jobTitle);
                        System.out.println("Quota:\t\t\t" + jobWorkload);
                        System.out.println("Company:\t\t" + jobOrganisation);
                        System.out.println("Languages:\t\t" + jobLanguage);*/
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
