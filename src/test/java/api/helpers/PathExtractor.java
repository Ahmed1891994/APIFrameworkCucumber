package api.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import java.util.Map;
import java.util.HashMap;

public class PathExtractor {
    private final ObjectMapper objectMapper;
    private Map<String, Object> extractedValues;

    public PathExtractor() {
        this.objectMapper = new ObjectMapper();
        this.extractedValues = new HashMap<>();
    }

    public void extractValue(String response, String jsonPath, String key) {
        Object value = JsonPath.read(response, jsonPath);
        extractedValues.put(key, value);
    }

    public void storeValue(String key, Object value) {
        extractedValues.put(key, value);
    }

    public Object getValue(String key) {
        return extractedValues.get(key);
    }

    public String buildUrl(String baseUrl, String endpoint, Map<String, String> pathParams) {
        String finalEndpoint = endpoint;
        if (pathParams != null) {
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                String paramValue = entry.getValue();
                if (extractedValues.containsKey(paramValue)) {
                    paramValue = String.valueOf(extractedValues.get(paramValue));
                }
                finalEndpoint = finalEndpoint.replace("{" + entry.getKey() + "}", paramValue);
            }
        }
        return baseUrl + finalEndpoint;
    }

    public void clear() {
        extractedValues.clear();
    }
}