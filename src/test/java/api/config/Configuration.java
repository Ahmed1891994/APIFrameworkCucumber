package api.config;

import api.exceptions.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Configuration {
    private Properties properties;

    public Configuration(String environment) {
        loadConfig(environment);
    }

    private void loadConfig(String env) {
        properties = new Properties();
        String configFile = "config/api-" + env + ".properties";

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input != null) {
                assert properties != null;
                properties.load(input);
            } else {
                throw new ConfigurationException("Config file not found: " + configFile);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load config: " + configFile, e);
        }
    }

    public String getBaseUrl() {
        return properties.getProperty("base.url", "");
    }

    public int getRequestTimeout() {
        try {
            return Integer.parseInt(properties.getProperty("request.timeout.seconds", "30"));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid timeout configuration", e);
        }
    }

    public int getResponseTimeout() {
        try {
            return Integer.parseInt(properties.getProperty("response.timeout.seconds", "30"));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid timeout configuration", e);
        }
    }

    public Map<String, String> getPropertiesWithPrefix(String prefix) {
        Map<String, String> result = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), properties.getProperty(key));
            }
        }
        return result;
    }

    public boolean isUrlEncodingEnabled() {
        return Boolean.parseBoolean(properties.getProperty("url.encoding.enabled", "true"));
    }
}