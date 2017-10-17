package ch.fhnw.jobannotations.measure;

import ch.fhnw.jobannotations.jobtitle.JobTitleExtractor;
import com.opencsv.CSVReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to measure precision and recall of:
 * - JobTitle extraction
 *
 * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
 */
public class Measure {

    private final String PATH_TO_TESTSUITE;

    private HashMap<String, String> testSuite;

    public Measure(String pathToTestsuite) {
        PATH_TO_TESTSUITE = pathToTestsuite;

        // initialize test suite
        testSuite = getJobTitleTestSuite(PATH_TO_TESTSUITE);
    }

    /**
     * Determine precision and recall of JobTitle extraction
     *
     * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
     */
    public void performJobTitleMeasurement() {

        int foundTitles = 0;

        JobTitleExtractor ex = new JobTitleExtractor();

        // for automated measurement, uncleaned titles are needed
        ex.setCleanJobTitle(false);

        for (Map.Entry<String, String> test : testSuite.entrySet()) {
            final String threshold = test.getKey();
            final String hyperlink = test.getValue();

            try {

                // load job document
                Document document = Jsoup.connect(hyperlink).get();

                // get most probable job title
                String foundTitle = ex.parseJobTitle(document);

                // check if found title matches threshold
                if (foundTitle.toLowerCase().equals(threshold.toLowerCase())) {
                    foundTitles++;
                } else {
                    System.err.println("\nFalse match:");
                    System.err.println("Threshold: " + threshold + " \nFound: " + foundTitle);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        System.out.println();
        System.err.println("Job documents processed: " + testSuite.size());
        System.err.println("Found titles: " + foundTitles);

        double percentage = foundTitles * 100 / testSuite.size();
        System.out.println("Success rate: " + percentage + "%");
    }

    /**
     * Get a list of hyperlinks and threshold JobTitle
     *
     * @return Key/Value pair containing URL and threshold JobTitle
     * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
     */
    private HashMap<String, String> getJobTitleTestSuite(String pathToTestFile) {

        final int THRESHOLD_POS = 0;
        final int URL_POS = 1;

        System.out.println("Loading testsuite...");

        HashMap<String, String> results = new HashMap<>();

        try {

            // read test file's contents
            CSVReader reader = new CSVReader(new FileReader(pathToTestFile), ';');

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                results.put(nextLine[THRESHOLD_POS], nextLine[URL_POS]);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // indicate if test suite was loaded successfully
        if (results != null) {
            System.out.println(PATH_TO_TESTSUITE + " contained " + results.size() + " test cases.");
            System.out.println();
        } else {
            System.out.println("Failed to load test suite from " + PATH_TO_TESTSUITE);
        }

        return results;
    }

}
