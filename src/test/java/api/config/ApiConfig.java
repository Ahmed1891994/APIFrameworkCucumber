package api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ApiConfig {
    private Properties properties;
    private final String environment;

    public ApiConfig() {
        this("dev"); // default to development environment
    }

    public ApiConfig(String environment) {
        this.environment = environment;
        loadConfig(environment);
        initializeLogging();
    }

    private void loadConfig(String env) {
        properties = new Properties();
        String configFile = "config/api-" + env + ".properties";

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input != null) {
                properties.load(input);
            } else {
                throw new RuntimeException("Config file not found: " + configFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + configFile, e);
        }
    }

    private void initializeLogging() {
        boolean debugMode = Boolean.parseBoolean(properties.getProperty("logging.debug", "false"));
//        LoggerConfig.initialize(debugMode);
    }

    public String getBaseUrl() {
        return properties.getProperty("base.url", "");
    }

    public int getTimeout() {
        return Integer.parseInt(properties.getProperty("timeout.seconds", "30"));
    }

    public int getMaxRetries() {
        return Integer.parseInt(properties.getProperty("max.retries", "3"));
    }

    public boolean isDebugMode() {
        return Boolean.parseBoolean(properties.getProperty("logging.debug", "false"));
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
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

    public String getEnvironment() {
        return environment;
    }
}