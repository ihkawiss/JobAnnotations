package ch.fhnw.jobannotations.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * @author Hoang
 */
public class FileUtils {
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
}
