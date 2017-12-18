package ch.fhnw.jobannotations.utils;

import org.apache.log4j.Logger;
import sun.rmi.runtime.Log;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/**
 * This class covers recurring file handling tasks.
 *
 * @author Hoang Tran <hoang.tran@students.fhnw.ch>
 */
public class FileUtils {

    final static Logger LOG = Logger.getLogger(FileUtils.class);

    private FileUtils() {
    }

    /**
     * Gets a stream to a file stored in resources of the project.
     *
     * @param path where file is located
     * @return InputStream to file
     */
    public static InputStream getResourceInputStream(String path) {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        return classLoader.getResourceAsStream(path);
    }

    /**
     * Gets a stream to a file path.
     *
     * @param path where file is located
     * @return InputStream to file
     * @throws IOException if file could not be read
     */
    public static InputStream getFileAsInputStream(String path) throws IOException {
        return Files.newInputStream(Paths.get(path));
    }

    /**
     * Write new data to train file. Duplicates will be deleted.
     *
     * @param data to learn
     */
    public static void addDataToTrainFile(String filename, String data) {

        try {

            // determine if quarantine is enabled
            boolean quarantine = ConfigurationUtil.get("configuration.ml.quarantine").equalsIgnoreCase("true");

            if (quarantine) {
                filename += ".quarantine";

                // make sure quarantine exists
                if (!Files.exists(Paths.get(filename))) {
                    Files.createFile(Paths.get(filename));
                }
            }

            // make sure duplicates get ignored
            HashSet<String> newContents = new HashSet<>();

            // reader to train or train.quarantine file
            BufferedReader br = new BufferedReader(new FileReader(filename));

            // read first line to process
            String line = br.readLine();

            // read content into set
            while (line != null) {
                newContents.add(line);

                line = br.readLine();
            }

            br.close();

            PrintWriter pw = new PrintWriter(filename);

            // persist content to file
            for (String entry : newContents) {
                pw.println(entry);
            }

            // print new data line
            pw.println(data);

            pw.flush();
            pw.close();

            LOG.debug("Saved found data in file " + filename);

        } catch (IOException e) {
            LOG.error("Something went wrong while learning new data.", e);
        }

    }

    /**
     * Gets configuration of external StanfordCoreNLP library
     *
     * @return Properties read from configuration file
     */
    public static Properties getStanfordCoreNLPGermanConfiguration() {

        try {
            final String STANFORD_CONFIGURATION = ConfigurationUtil.get("external.StanfordCoreNLP.configuration");

            BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.getFileAsInputStream(STANFORD_CONFIGURATION), "UTF8"));

            Properties props = new Properties();
            props.load(reader);

            return props;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Gets a List containing any line the file contains.
     *
     * @param filename to parse
     * @return List of lines
     */
    public static List<String> getFileContentAsList(String filename) {

        if (filename == "" || filename == null)
            throw new IllegalArgumentException("Filename must not be empty!");

        try {

            // initialize reader to file
            BufferedReader r = new BufferedReader(new InputStreamReader(getFileAsInputStream(filename)));

            // prepare result list
            List<String> result = new ArrayList<>();

            String line;
            while ((line = r.readLine()) != null) {
                result.add(line);
            }

            return result;

        } catch (IOException e) {
            LOG.error("Something went wrong while reading the following file: " + filename);
        }

        return null;
    }

}
