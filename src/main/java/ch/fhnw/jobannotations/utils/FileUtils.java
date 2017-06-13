package ch.fhnw.jobannotations.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;

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
}
