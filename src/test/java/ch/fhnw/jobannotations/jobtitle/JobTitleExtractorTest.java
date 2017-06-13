package ch.fhnw.jobannotations.jobtitle;

import org.jsoup.Jsoup;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Hoang
 */
public class JobTitleExtractorTest {
    JobTitleExtractor testee = new JobTitleExtractor();

    @Test
    public void testParseJobTitleParser() {
        Map<String, String> jobTitleWebPages = new HashMap<>();
        jobTitleWebPages.put("http://ictjobs.ch/software-entwicklung/scrum-master-mw-80-100/", "Scrum Master");
        jobTitleWebPages.put("http://ictjobs.ch/beratung-consultants/agile-requirement-engineer-upfront-thinker-mw-80-100/", "Agile Requirement Engineer / Upfront Thinker");
        jobTitleWebPages.put("http://ictjobs.ch/beratung-consultants/senior-business-analyst-requirement-engineering-mw-2/", "Senior Business Analyst (Requirement Engineering)");
        jobTitleWebPages.put("http://ictjobs.ch/system-netzwerktechnik-security-voip/ict-system-engineer-vdi-100-2/", "ICT System Engineer (VDI)");
        jobTitleWebPages.put("http://ictjobs.ch/system-netzwerktechnik-security-voip/junior-cloud-engineer-office-365-enterprise-wm/", "(Junior) Cloud Engineer Office 365 Enterprise");
        jobTitleWebPages.put("http://ictjobs.ch/software-entwicklung/innovation-software-engineer-uxud/", "Innovation Software Engineer (UX/UD)");
        jobTitleWebPages.put("http://ictjobs.ch/software-entwicklung/performance-and-capacity-software-engineer/", "Performance and Capacity Software Engineer");
        jobTitleWebPages.put("http://ictjobs.ch/software-entwicklung/devops-engineer-mw-80-100/", "DevOps Engineer");
        jobTitleWebPages.put("http://ictjobs.ch/support-it-services/ict-supporterin-80/", "ICT-Supporter/in");
        jobTitleWebPages.put("http://ictjobs.ch/support-it-services/applikationsverantwortlicher-sap-is-h-80-100/", "Applikationsverantwortliche/r SAP IS-H");
        jobTitleWebPages.put("http://www.jobs.ch/de/stellenangebote/detail/7541447/?source=vacancy_search", "Art Director");
        jobTitleWebPages.put("http://www.jobs.ch/de/stellenangebote/detail/7540578/?source=vacancy_search", "Praktikantin / Praktikant Textildesign");
        jobTitleWebPages.put("http://www.jobs.ch/de/stellenangebote/detail/7538684/?source=vacancy_search", "Jewelry Designer");

        for (Map.Entry<String, String> entry : jobTitleWebPages.entrySet()) {
            String jobTitle = testee.parseJobTitle(Jsoup.parse(entry.getKey()));
            assertEquals(entry.getValue(), jobTitle);
        }
    }

}
