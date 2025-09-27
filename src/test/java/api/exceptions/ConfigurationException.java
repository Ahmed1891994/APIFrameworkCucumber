package api.exceptions;

public class ConfigurationException extends ApiFrameworkException {
    public ConfigurationException(String message) {
        super(message, "CONFIG_001", "Configuration");
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, "CONFIG_001", "Configuration", cause);
    }

    // Specific configuration errors
    public static ConfigurationException missingProperty(String propertyName) {
        return new ConfigurationException("Required property not found: " + propertyName);
    }

    public static ConfigurationException invalidProperty(String propertyName, String value, String details) {
        return new ConfigurationException(
                String.format("Invalid value '%s' for property '%s': %s", value, propertyName, details)
        );
    }

    public static ConfigurationException invalidProperty(String propertyName, String value, String details, Throwable cause) {
        return new ConfigurationException(
                String.format("Invalid value '%s' for property '%s': %s", value, propertyName, details),
                cause
        );
    }

    public static ConfigurationException fileNotFound(String configFile) {
        return new ConfigurationException("Configuration file not found: " + configFile);
    }
}