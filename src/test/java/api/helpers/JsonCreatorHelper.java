package api.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class JsonCreatorHelper {
    private static final Logger logger = LoggerFactory.getLogger(JsonCreatorHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Adds values to ObjectNode AND returns converted values for storage
     */
    public static void addCucumberValues(ObjectNode target, Map<String, String> values) {
        if (values == null || values.isEmpty()) return;

        values.forEach((key, value) -> {
            Object convertedValue = convertCucumberValue(value);
            addValue(target, key, convertedValue);
        });
    }

    /**
     * Converts Cucumber table string values to appropriate data types
     */
    public static Object convertCucumberValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String trimmedValue = value.trim();

        return switch (determineValueType(trimmedValue)) {
            case BOOLEAN -> Boolean.parseBoolean(trimmedValue);
            case INTEGER -> parseBigInteger(trimmedValue);
            case DECIMAL -> parseBigDecimal(trimmedValue);
            case JSON -> parseJson(trimmedValue);  // ← Changed from ARRAY to JSON
            case QUOTED_STRING -> trimmedValue.substring(1, trimmedValue.length() - 1);
            case STRING -> trimmedValue;
        };
    }

    private enum ValueType { BOOLEAN, INTEGER, DECIMAL, JSON, QUOTED_STRING, STRING }

    private static ValueType determineValueType(String value) {
        if (isBoolean(value)) return ValueType.BOOLEAN;
        if (isQuotedString(value)) return ValueType.QUOTED_STRING;
        if (isJson(value)) return ValueType.JSON;  // ← Changed from ARRAY to JSON
        if (isInteger(value)) return ValueType.INTEGER;
        if (isDecimal(value)) return ValueType.DECIMAL;
        return ValueType.STRING;
    }


    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean isQuotedString(String value) {
        return value.startsWith("\"") && value.endsWith("\"") && value.length() > 1;
    }

    private static boolean isJson(String value) {
        return (value.startsWith("[") && value.endsWith("]") && value.length() > 1) ||
                (value.startsWith("{") && value.endsWith("}") && value.length() > 1);
    }

    private static boolean isInteger(String value) {
        return value.matches("-?\\d+");
    }

    private static boolean isDecimal(String value) {
        return value.matches("-?\\d+\\.\\d+");
    }

    private static Object parseBigInteger(String value) {
        try {
            return new BigInteger(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse integer, keeping as string: {}", value);
            return value;
        }
    }

    private static Object parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse decimal, keeping as string: {}", value);
            return value;
        }
    }

    private static Object parseJson(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (Exception e) {
            logger.warn("Failed to parse JSON: {}, keeping as string", value);
            return value;
        }
    }

    /**
     * Adds a value to ObjectNode with comprehensive type handling
     */
    public static void addValue(ObjectNode target, String key, Object value) {
        if (value == null) {
            logger.warn("Null value for key: {}", key);
            return;
        }

        try {
            switch (value) {
                case String s -> target.put(key, s);
                case Number n -> target.putPOJO(key, n);
                case Boolean b -> target.put(key, b);
                case Object[] array -> target.set(key, objectMapper.valueToTree(array));
                case List<?> list -> target.set(key, objectMapper.valueToTree(list));
                case Map<?, ?> map -> target.set(key, objectMapper.valueToTree(map));
                default -> target.set(key, objectMapper.valueToTree(value));
            }
            logger.info("Added body parameter - {}: {}", key, value);
        } catch (Exception ex) {
            logger.error("Failed to add body parameter for key {}: {}", key, ex.getMessage(), ex);
        }
    }

    public static void removeKeys(ObjectNode target, List<String> keys) {
        if (keys == null || keys.isEmpty()) return;

        keys.forEach(key -> {
            if (target.has(key)) {
                target.remove(key);
                logger.info("Removed key from body: {}", key);
            } else {
                logger.warn("Tried to remove non-existent key: {}", key);
            }
        });
    }
}