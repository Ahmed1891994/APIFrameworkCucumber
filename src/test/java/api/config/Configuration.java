package api.config;

import api.exceptions.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
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
                // Validate required properties after loading
                validateRequiredProperties();
            } else {
                throw ConfigurationException.fileNotFound(configFile);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load config: " + configFile, e);
        }
    }

    private void validateRequiredProperties() {
        // Check for required properties
        if (!properties.containsKey("base.url")) {
            throw ConfigurationException.missingProperty("base.url");
        }

        // Validate timeout values are positive
        validateTimeoutProperty("request.timeout.seconds");
        validateTimeoutProperty("response.timeout.seconds");
    }

    private void validateTimeoutProperty(String propertyName) {
        String value = properties.getProperty(propertyName);
        if (value != null) {
            try {
                int timeout = Integer.parseInt(value);
                if (timeout <= 0) {
                    throw ConfigurationException.invalidProperty(propertyName, value,
                            "Timeout must be a positive integer");
                }
            } catch (NumberFormatException e) {
                throw ConfigurationException.invalidProperty(propertyName, value,
                        "Must be a valid integer", e);
            }
        }
    }

    public String getBaseUrl() {
        String baseUrl = properties.getProperty("base.url", "");
        if (baseUrl.isEmpty()) {
            throw ConfigurationException.missingProperty("base.url");
        }
        return baseUrl;
    }

    public int getRequestTimeout() {
        try {
            String timeoutStr = properties.getProperty("request.timeout.seconds", "30");
            int timeout = Integer.parseInt(timeoutStr);
            if (timeout <= 0) {
                throw ConfigurationException.invalidProperty("request.timeout.seconds", timeoutStr,
                        "Timeout must be positive");
            }
            return timeout;
        } catch (NumberFormatException e) {
            String actualValue = properties.getProperty("request.timeout.seconds");
            throw ConfigurationException.invalidProperty("request.timeout.seconds",
                    actualValue != null ? actualValue : "null", "Must be a valid integer", e);
        }
    }

    public int getResponseTimeout() {
        try {
            String timeoutStr = properties.getProperty("response.timeout.seconds", "30");
            int timeout = Integer.parseInt(timeoutStr);
            if (timeout <= 0) {
                throw ConfigurationException.invalidProperty("response.timeout.seconds", timeoutStr,
                        "Timeout must be positive");
            }
            return timeout;
        } catch (NumberFormatException e) {
            String actualValue = properties.getProperty("response.timeout.seconds");
            throw ConfigurationException.invalidProperty("response.timeout.seconds",
                    actualValue != null ? actualValue : "null", "Must be a valid integer", e);
        }
    }

    public boolean isUrlEncodingEnabled() {
        String value = properties.getProperty("url.encoding.enabled", "true");
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw ConfigurationException.invalidProperty("url.encoding.enabled", value,
                    "Must be 'true' or 'false'");
        }
        return Boolean.parseBoolean(value);
    }

    public int getThreadCount() {
        try {
            String threadStr = properties.getProperty("parallel.threads", "1");
            int threads = Integer.parseInt(threadStr);
            if (threads < 1) {
                throw ConfigurationException.invalidProperty("parallel.threads", threadStr,
                        "Thread count must be at least 1");
            }
            return threads;
        } catch (NumberFormatException e) {
            String actualValue = properties.getProperty("parallel.threads");
            throw ConfigurationException.invalidProperty("parallel.threads",
                    actualValue != null ? actualValue : "null", "Must be a valid integer", e);
        }
    }

    public String getParallelMode() {
        String mode = properties.getProperty("parallel.mode", "scenarios");
        if (!mode.equals("scenarios") && !mode.equals("features") && !mode.equals("methods")) {
            throw ConfigurationException.invalidProperty("parallel.mode", mode,
                    "Must be 'scenarios', 'features', or 'methods'");
        }
        return mode;
    }

    public boolean isParallelEnabled() {
        String value = properties.getProperty("parallel.enabled", "false");
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw ConfigurationException.invalidProperty("parallel.enabled", value,
                    "Must be 'true' or 'false'");
        }
        return Boolean.parseBoolean(value);
    }

    public int getDataProviderThreadCount() {
        try {
            String threadStr = properties.getProperty("dataprovider.threads", "10");
            int threads = Integer.parseInt(threadStr);
            if (threads < 1) {
                throw ConfigurationException.invalidProperty("dataprovider.threads", threadStr,
                        "DataProvider thread count must be at least 1");
            }
            return threads;
        } catch (NumberFormatException e) {
            String actualValue = properties.getProperty("dataprovider.threads");
            throw ConfigurationException.invalidProperty("dataprovider.threads",
                    actualValue != null ? actualValue : "null", "Must be a valid integer", e);
        }
    }
}