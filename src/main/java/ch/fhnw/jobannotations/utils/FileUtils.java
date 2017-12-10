package ch.fhnw.jobannotations.utils;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Hoang
 */
public class FileUtils {

    final static Logger LOG = Logger.getLogger(FileUtils.class);

    private FileUtils() {
        // private constructor
    }

    public static InputStream getFileAsInputStream(String fileName) {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }

    public static Properties getStanfordCoreNLPGermanConfiguration() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.getFileAsInputStream("configuration/StanfordCoreNLP-german.properties"), "UTF8"));

            Properties props = new Properties();
            props.load(reader);

            return props;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

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
