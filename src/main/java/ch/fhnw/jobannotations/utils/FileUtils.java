package ch.fhnw.jobannotations.utils;

import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 * @author Hoang
 */
public class FileUtils {
    private FileUtils() {
        // private constructor
    }

    public static FileInputStream getFileInputStream(String fileName) {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource != null) {
            try {
                return new FileInputStream(resource.getFile());
            } catch (FileNotFoundException e) {
                // ignore
            }
        }
        return null;
    }

    public static Properties getStanfordCoreNLPGermanConfiguration() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.getFileInputStream("configuration/StanfordCoreNLP-german.properties"), "UTF8"));

            Properties props = new Properties();
            props.load(reader);

            return props;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
