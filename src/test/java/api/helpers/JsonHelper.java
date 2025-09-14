package api.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonHelper {
    private static final Logger logger = LoggerFactory.getLogger(JsonHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Adds a value into the given ObjectNode safely.
     * Supports numbers, booleans, arrays, JSON strings, lists, and POJOs.
     */
    public static void addValue(ObjectNode target, String key, Object value) {
        if (value == null) {
            logger.warn("null value for key: {}", key);
            return;
        }
        Object val = castValue(value);
        switch (val) {
            case String strVal -> {
                target.put(key, strVal);
                logger.info("added plain string body parameter - {}: {}", key, strVal);
            }
            case ObjectNode node -> {
                target.set(key, node);
                logger.info("added JSON body parameter - {}: {}", key, node.asText());
            }
            case Long intVal -> target.put(key, intVal);
            case Boolean boolVal -> target.put(key, boolVal);
            case ArrayList<?> array -> target.set(key, objectMapper.valueToTree(array));
            default -> target.putPOJO(key, val);
        }
    }

    private static Object castValue(Object val) {
        String value = (String) val;
        value = value.trim();

        if (value.startsWith("[") && value.endsWith("]")) {
            String arrayContent = value.substring(1, value.length() - 1).trim();
            if (arrayContent.isEmpty()) return new ArrayList<>();

            List<String> items = Arrays.stream(arrayContent.split(","))
                    .map(String::trim)
                    .map(s -> s.replaceAll("^\"|\"$", ""))
                    .collect(Collectors.toList());

            if (items.stream().allMatch(s -> s.matches("-?\\d+"))) {
                return items.stream().map(Integer::valueOf).collect(Collectors.toList());
            } else if (items.stream().allMatch(s -> s.matches("-?\\d*\\.\\d+"))) {
                return items.stream().map(Double::valueOf).collect(Collectors.toList());
            } else {
                return items;
            }
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        if (value.matches("-?\\d+")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return Long.parseLong(value);
            }
        }

        if (value.matches("-?\\d*\\.\\d+")) {
            return Double.parseDouble(value);
        }

        return value;
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
            logger.info("Removed key from body: {}", key);
        } else {
            logger.warn("Tried to remove non-existent key: {}", key);
        }
    }

}
