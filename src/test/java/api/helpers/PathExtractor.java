package api.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathExtractor {
    private static final Logger logger = LoggerFactory.getLogger(PathExtractor.class);
    private final Configuration jacksonConfig;
    private final ObjectMapper objectMapper;

    public PathExtractor() {
        objectMapper = new ObjectMapper();
        this.jacksonConfig = Configuration.builder()
                .jsonProvider(new com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider(objectMapper))
                .mappingProvider(new com.jayway.jsonpath.spi.mapper.JacksonMappingProvider(objectMapper))
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
    }

    /**
     * Extracts a value from JSON response using JSONPath and returns it (doesn't store it)
     */
    public Object extractValue(String response, String jsonPath, String key) {
        try {
            JsonNode result = JsonPath.using(jacksonConfig).parse(response).read(jsonPath, JsonNode.class);
            Object convertedValue = convertJsonNode(result);
            logger.info("📤 Extracted value from JSON path '{}': {} -> {}", jsonPath, key, convertedValue);
            return convertedValue;
        } catch (PathNotFoundException e) {
            logger.warn("❌ JSON path '{}' not found in response", jsonPath);
            throw new RuntimeException("JSON path '" + jsonPath + "' not found in response", e);
        } catch (Exception e) {
            logger.error("❌ Failed to extract value from path '{}': {}", jsonPath, e.getMessage());
            throw new RuntimeException("Failed to extract value from path: " + jsonPath, e);
        }
    }

    /**
     * Reads a value from JSON using JSONPath without storing it
     */
    public Object readJsonPath(String response, String jsonPath) {
        try {
            JsonNode result = JsonPath.using(jacksonConfig).parse(response).read(jsonPath, JsonNode.class);
            Object value = convertJsonNode(result);
            logger.debug("📖 Read JSON path '{}': {}", jsonPath, value);
            return value;
        } catch (PathNotFoundException e) {
            logger.debug("JSON path '{}' not found, returning null", jsonPath);
            return null;
        } catch (Exception e) {
            logger.error("❌ Failed to read JSON path '{}': {}", jsonPath, e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a JSON path exists in the response
     */
    public boolean existsJsonPath(String json, String jsonPath) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            if (jsonPath.startsWith("$.")) {
                String fieldName = jsonPath.substring(2);
                boolean exists = jsonNode.has(fieldName);
                logger.debug("🔍 JSON path '{}' exists: {}", jsonPath, exists);
                return exists;
            }
            boolean isRoot = jsonPath.equals("$");
            logger.debug("🔍 JSON path '{}' is root: {}", jsonPath, isRoot);
            return isRoot;
        } catch (Exception e) {
            logger.warn("⚠️ Error checking JSON path existence '{}': {}", jsonPath, e.getMessage());
            return false;
        }
    }

    /**
     * Converts JsonNode to appropriate Java type
     */
    private Object convertJsonNode(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.textValue();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isArray() || node.isObject()) return node.toString();
        return node.asText();
    }
}