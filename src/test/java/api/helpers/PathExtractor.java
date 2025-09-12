package api.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class PathExtractor {
    private final Map<String, Object> extractedValues = new HashMap<>();
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

    public void extractValue(String response, String jsonPath, String key) {
        try {
            JsonNode result = JsonPath.using(jacksonConfig).parse(response).read(jsonPath, JsonNode.class);
            extractedValues.put(key, convertJsonNode(result));
        } catch (PathNotFoundException e) {
            throw new RuntimeException("JSON path '" + jsonPath + "' not found in response", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract value from path: " + jsonPath, e);
        }
    }

    private Object convertJsonNode(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.textValue();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isArray() || node.isObject()) return node.toString();
        return node.asText();
    }

    public void storeValue(String key, Object value) {
        extractedValues.put(key, value);
    }

    public Object getValue(String key) {
        return extractedValues.get(key);
    }

    public boolean hasValue(String key) {
        return extractedValues.containsKey(key);
    }

    public String buildUrl(String baseUrl, String endpoint, Map<String, String> pathParams) {
        String finalEndpoint = endpoint;

        // First use explicit path parameters
        if (pathParams != null) {
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                String paramValue = entry.getValue();
                if (extractedValues.containsKey(paramValue)) {
                    paramValue = String.valueOf(extractedValues.get(paramValue));
                }
                finalEndpoint = finalEndpoint.replace("{" + entry.getKey() + "}", paramValue);
            }
        }

        for (Map.Entry<String, Object> entry : extractedValues.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (finalEndpoint.contains(placeholder) && entry.getValue() != null) {
                finalEndpoint = finalEndpoint.replace(placeholder, entry.getValue().toString());
            }
        }

        return baseUrl + finalEndpoint;
    }

    public void clear() {
        extractedValues.clear();
    }

    public Map<String, Object> getAllValues() {
        return new HashMap<>(extractedValues);
    }

    public Object readJsonPath(String response, String jsonPath) {
        try {
            JsonNode result = JsonPath.using(jacksonConfig).parse(response).read(jsonPath, JsonNode.class);
            return convertJsonNode(result);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    public boolean existsJsonPath(String json, String jsonPath) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);

            // Handle simple paths like "$.extraField", "$.name", etc.
            if (jsonPath.startsWith("$.")) {
                String fieldName = jsonPath.substring(2); // Remove "$." prefix
                return jsonNode.has(fieldName);
            }

            // Handle root level access "$"
            return jsonPath.equals("$"); // Root always exists

            // For array access like "$[0]", you'd need more complex logic
            // For now, let's keep it simple and return false for complex paths

        } catch (Exception e) {
            return false;
        }
    }

    public Configuration getJacksonConfig() {
        return jacksonConfig;
    }

    public String resolvePlaceholders(String text) {
        if (text == null) return null;

        String result = text;
        for (Map.Entry<String, Object> entry : extractedValues.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                result = result.replace("${" + entry.getKey() + "}", value.toString());
            }
        }
        return result;
    }
}
