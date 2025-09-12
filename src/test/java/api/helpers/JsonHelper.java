package api.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class JsonHelper {
    private static final Logger logger = LoggerFactory.getLogger(JsonHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Adds a value into the given ObjectNode safely.
     * Supports numbers, booleans, arrays, JSON strings, lists, and POJOs.
     */
    public static void addValue(ObjectNode target, String key, Object value) {
        if (value == null) {
            logger.warn("Null value for key: {}", key);
            return;
        }

        try {
            if (value instanceof String strVal) {
                JsonNode node = tryParseJson(strVal);
                if (node != null) {
                    target.set(key, node);
                    logger.debug("Added JSON body parameter - {}: {}", key, strVal);
                } else {
                    target.put(key, strVal);
                    logger.debug("Added plain string body parameter - {}: {}", key, strVal);
                }
            } else if (value instanceof Number) {
                target.putPOJO(key, value);
            } else if (value instanceof Boolean boolVal) {
                target.put(key, boolVal);
            } else if (value instanceof List || value.getClass().isArray()) {
                target.set(key, objectMapper.valueToTree(value));
            } else {
                target.putPOJO(key, value);
            }
        } catch (Exception ex) {
            logger.error("Failed to add body parameter for key {}: {}", key, ex.getMessage(), ex);
        }
    }

    private static JsonNode tryParseJson(String strVal) {
        try {
            return objectMapper.readTree(strVal); // Direct parse
        } catch (Exception e1) {
            try {
                String normalized = strVal.replace('\'', '"'); // Normalize single quotes
                return objectMapper.readTree(normalized);
            } catch (Exception e2) {
                return null; // Not valid JSON
            }
        }
    }

    /**
     * Adds multiple values into the given ObjectNode.
     */
    public static void mergeJson(ObjectNode target, Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.forEach((key, value) -> addValue(target, key, value));
    }

    public static void removeKey(ObjectNode target, String key) {
        if (target.has(key)) {
            target.remove(key);
            logger.debug("Removed key from body: {}", key);
        } else {
            logger.warn("Tried to remove non-existent key: {}", key);
        }
    }

}
