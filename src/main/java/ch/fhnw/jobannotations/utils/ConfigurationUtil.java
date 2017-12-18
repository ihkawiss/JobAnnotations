package ch.fhnw.jobannotations.utils;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class it responsible to ensure access to the application's
 * configurations, defined in the job-annotations.properties file.
 * Those settings determine how this library is supposed to work.
 *
 * @author Kevin Kirn <kevin.kirn@students.fhnw.ch>
 */
public final class ConfigurationUtil {

    private final static Logger LOG = Logger.getLogger(ConfigurationUtil.class);
    private final static String CONFIGURATION_FILE = "job-annotations.properties";

    private boolean debugModeEnabled;
    private static ConfigurationUtil instance;
    private Properties properties;

    private ConfigurationUtil() {

        try {

            InputStream input = FileUtils.getResourceInputStream(CONFIGURATION_FILE);
            properties = new Properties();
            properties.load(input);

            // load debug mode
            debugModeEnabled = properties.get("configuration.debug.enabled").toString().equalsIgnoreCase("true");

        } catch (IOException e) {
            LOG.error("Failed to load " + CONFIGURATION_FILE + ". Please see documentation.");
        }

    }

    private static ConfigurationUtil getInstance() {

        if (instance == null)
            instance = new ConfigurationUtil();

        return instance;
    }

    /**
     * Wrapper to grab properties from holder
     *
     * @param key the desired property name
     * @return the value of a found key
     */
    public static String get(String key) {
        return getInstance().properties.getProperty(key);
    }

    public static boolean isDebugModeEnabled() {
        return getInstance().debugModeEnabled;
    }

}
