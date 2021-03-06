package org.tweetwallfx.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public final class Configuration {

    private static final Logger LOGGER = Logger.getLogger(Configuration.class);
    private static final Configuration INSTANCE = new Configuration();
    private Properties props = new Properties();

    private Configuration() {
        final File log4jFile = new File("log4j.xml");
        
        if (log4jFile.isFile()) {
            DOMConfigurator.configure("log4j.xml");
        } else {
            LOGGER.info("log4j configuration file ('" + log4jFile.getAbsolutePath() + "') found.");
        }

        LOGGER.info("Searching for configuration files in path '/config.properties'");

        try {
            final Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources("config.properties");

            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                LOGGER.info("Found config file: " + url);

                try (final InputStream is = url.openStream()) {
                    props.load(is);
                }
            }
            
            final File overrideFile = new File("config.properties");

            if (overrideFile.isFile()) {
                try (final InputStream is = overrideFile.toURI().toURL().openStream()) {
                    props.load(is);
                }
            } else {
                LOGGER.info("config override file ('" + overrideFile.getAbsolutePath() + "') found.");
            }
        } catch (final IOException ioe) {
            throw new RuntimeException("Error initiating Configuration", ioe);
        }

        LOGGER.info("Configurations:");
        props.entrySet()
                .stream()
                .map(e -> String.format("'%s' -> '%s'", String.valueOf(e.getKey()), String.valueOf(e.getValue())))
                .forEach(LOGGER::info);
    }

    public static Configuration getInstance() {
        return INSTANCE;
    }

    public String getConfig(final String param) {
        return Objects.requireNonNull(props.getProperty(param), "Configuration for '" + param + "' does not exist");
    }

    public String getConfig(final String param, final String defaultValue) {
        return props.getProperty(param, defaultValue);
    }
}
