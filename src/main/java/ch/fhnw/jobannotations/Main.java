package ch.fhnw.jobannotations;

import ch.fhnw.jobannotations.utils.ConfigurationUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Map;

/**
 * @author Hoang
 */
public class Main {

    public static final boolean DEBUG = ConfigurationUtil.isDebugModeEnabled();

    public static void main(String[] args) {
        boolean keepGoing = true;

        // print welcome message
        System.out.println(StringUtils.repeat("-", 80));
        System.out.println("Welcome to JobAnnotations!\n");
        System.out.println("Project:\tJobAnnotations FHNW IP5 2017");
        System.out.println("Authors:\tHoang Tran, Kevin Kirn");
        System.out.println(StringUtils.repeat("-", 80));

        if (DEBUG) {
            System.out.println(StringUtils.repeat("-", 80));
            System.out.println("NOTICE: DEBUG mode is enabled, extractors will print some messages.");
        }

        JobAnnotator annotator = new JobAnnotator();

        while (keepGoing) {

            boolean repeat = true;

            while (repeat) {
                System.out.println(StringUtils.repeat("-", 80) + "\n");
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

                        Map<String, String> results = annotator.parse(url);

                        // print report
                        System.out.println("\n" + StringUtils.repeat("-", 80));
                        System.out.println("RESULT REPORT");
                        System.out.println(StringUtils.repeat("-", 80));

                        for(Map.Entry<String, String> entry : results.entrySet()) {
                            System.out.println(entry.getKey() + ":\n" + entry.getValue() + "\n");
                        }

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
