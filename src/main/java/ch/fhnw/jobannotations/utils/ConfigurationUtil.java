package ch.fhnw.jobannotations.utils;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class ConfigurationUtil {

    private final static Logger LOG = Logger.getLogger(ConfigurationUtil.class);

    private static ConfigurationUtil instance;

    private Properties p;

    private ConfigurationUtil() {

        try {

            FileInputStream input = new FileInputStream("job-annotations.properties");

            p.load(input);

        } catch (IOException e) {
            LOG.error("Failed to load ./job-annotations.properties. Please see documentation.");
        }

    }

    public static ConfigurationUtil getInstance() {

        if (instance == null)
            instance = new ConfigurationUtil();

        return instance;
    }

    public String get(String key) {
        return p.getProperty(key);
    }

}
