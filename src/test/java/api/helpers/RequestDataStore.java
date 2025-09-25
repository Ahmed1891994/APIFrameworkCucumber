package api.helpers;

import api.config.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RequestDataStore {
    private static final Logger logger = LoggerFactory.getLogger(RequestDataStore.class);

    private final Map<String, Map<String, String>> requestHeadersMap = new HashMap<>();
    private final Map<String, ObjectNode> requestJsonBodyMap = new ConcurrentHashMap<>();
    private final Map<String, Object> contextValues = new ConcurrentHashMap<>();
    private final Configuration configuration;
    private Boolean requestSpecificUrlEncoding = null;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String lastHeaderSet;
    private String lastBodySet;
    private String endpoint;
    private String authType;

    public RequestDataStore(Configuration configuration) {
        this.configuration = configuration;
        initializeDefaults();
        logger.debug("Initialized RequestDataStore with URL encoding enabled: {}", configuration.isUrlEncodingEnabled());
    }

    /**
     * Clears all stored data and resets to initial state
     * Use this method in @Before hooks to ensure clean state for each test
     */
    public void clearAll() {
        int headersCount = requestHeadersMap.size();
        int bodiesCount = requestJsonBodyMap.size();
        int contextCount = contextValues.size();

        requestHeadersMap.clear();
        requestJsonBodyMap.clear();
        contextValues.clear();
        requestSpecificUrlEncoding = null;

        endpoint = null;
        authType = null;

        initializeDefaults();

        logger.info("Cleared all request data: {} header sets, {} bodies, {} context values. Reset to defaults.",
                headersCount, bodiesCount, contextCount);
    }

    /**
     * Set URL encoding for the next request only
     */
    public void setUrlEncodingForNextRequest(boolean enabled) {
        this.requestSpecificUrlEncoding = enabled;
        logger.debug("URL encoding for next request set to: {}", enabled);
    }

    /**
     * Determine if URL encoding should be applied
     */
    private boolean shouldEncodeUrl(boolean methodParameter) {
        // Priority: 1. Method parameter 2. Request-specific 3. Global config
        if (!methodParameter) return false;
        if (requestSpecificUrlEncoding != null) return requestSpecificUrlEncoding;
        return configuration.isUrlEncodingEnabled();
    }

    private void initializeDefaults() {
        requestHeadersMap.put("default", new HashMap<>());
        requestJsonBodyMap.put("default", objectMapper.createObjectNode());
        lastBodySet = "default";
        lastHeaderSet = "default";
    }

    public void addHeaders(String headerSetName, Map<String, String> additionalHeaders) {
        headerSetName = getHeaderDefault(headerSetName);
        Map<String, String> currentSet = requestHeadersMap.computeIfAbsent(headerSetName, _ -> new HashMap<>());
        currentSet.putAll(additionalHeaders);
        lastHeaderSet = headerSetName;
        logger.debug("Added {} headers to set '{}'", additionalHeaders.size(), headerSetName);
    }

    public Map<String, String> getHeaders(String headerSetName) {
        headerSetName = getHeaderDefault(headerSetName);
        Map<String, String> storedMap = requestHeadersMap.get(headerSetName);
        logger.debug("Retrieved headers from set '{}': {} headers", headerSetName,
                storedMap != null ? storedMap.size() : 0);
        return storedMap != null ? new HashMap<>(storedMap) : new HashMap<>();
    }

    public void clearHeaders(String headerSetName) {
        if (headerSetName == null) {
            headerSetName = getHeaderDefault(null);
        }
        requestHeadersMap.put(headerSetName, new ConcurrentHashMap<>());
        logger.debug("Cleared headers for set '{}'", headerSetName);
    }

    public void removeHeaders(String headerSetName, List<String> headerKeys) {
        headerSetName = getHeaderDefault(headerSetName);
        Map<String, String> headerSet = requestHeadersMap.get(headerSetName);
        if (headerSet != null && headerKeys != null) {
            int beforeSize = headerSet.size();
            headerKeys.forEach(headerSet::remove);
            logger.debug("Removed {} headers from set '{}'", beforeSize - headerSet.size(), headerSetName);
        }
    }

    public String getLastHeaderSet() {
        return lastHeaderSet;
    }

    public void setJsonBody(String bodyName, ObjectNode jsonBody) {
        requestJsonBodyMap.put(bodyName, jsonBody);
        lastBodySet = bodyName;
        logger.debug("Set JSON body '{}' with {} fields", bodyName, jsonBody.size());
    }

    public String getJsonBody(String bodyName) {
        if (bodyName == null) {
            return null;
        }
        ObjectNode node = requestJsonBodyMap.get(bodyName);
        logger.debug("Retrieved JSON body '{}': {}", bodyName, node != null);
        return node != null ? node.toString() : null;
    }

    public void clearBody(String bodyName) {
        requestJsonBodyMap.remove(bodyName);
        logger.debug("Cleared JSON body '{}'", bodyName);
    }

    public void setEmptyJsonBody(String bodyName) {
        requestJsonBodyMap.put(bodyName, objectMapper.createObjectNode());
        lastBodySet = bodyName;
        logger.debug("Set empty JSON body '{}'", bodyName);
    }

    public ObjectNode getOrCreateJsonBodyNode(String bodyName) {
        ObjectNode node = requestJsonBodyMap.computeIfAbsent(bodyName,
                _ -> objectMapper.createObjectNode());
        logger.debug("Get or create JSON body node '{}'", bodyName);
        return node;
    }

    public String getLastBodySet() {
        return lastBodySet;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        logger.debug("Set endpoint to: {}", endpoint);
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
        logger.debug("Set auth type to: {}", authType);
    }

    private String getHeaderDefault(String headerSetName) {
        if (headerSetName == null || headerSetName.trim().isEmpty()) {
            return "default";
        }
        return headerSetName;
    }

    public void removeKeysFromBody(String bodyName, List<String> keys) {
        ObjectNode jsonBody = requestJsonBodyMap.get(bodyName);
        if (jsonBody != null && keys != null) {
            int beforeSize = jsonBody.size();
            JsonCreatorHelper.removeKeys(jsonBody, keys);
            logger.debug("Removed {} keys from body '{}'", beforeSize - jsonBody.size(), bodyName);
        }
    }

    public void storeContextValue(String key, Object value) {
        contextValues.put(key, value);
        logger.debug("Stored context value '{}': {}", key, value);
    }

    public Object getContextValue(String key) {
        Object value = contextValues.get(key);
        logger.debug("Retrieved context value '{}': {}", key, value != null);
        return value;
    }

    public Map<String, Object> getAllContextValues() {
        logger.debug("Retrieved all {} context values", contextValues.size());
        return new HashMap<>(contextValues);
    }

    public String buildUrl(String baseUrl, String endpoint, Map<String, String> pathParams,
                           Map<String, String> queryParams, boolean encodeUrlParam) {

        boolean shouldEncode = shouldEncodeUrl(encodeUrlParam);
        logger.debug("URL encoding - Global: {}, Request-specific: {}, Param: {}, Final: {}",
                configuration.isUrlEncodingEnabled(), requestSpecificUrlEncoding,
                encodeUrlParam, shouldEncode);

        String finalEndpoint = endpoint;
        logger.debug("Building URL from base: {}, endpoint: {}", baseUrl, endpoint);

        // 1. Handle context value placeholders first (both {placeholder} and ${placeholder})
        finalEndpoint = replaceContextPlaceholders(finalEndpoint, shouldEncode);

        // 2. Handle explicit path parameters (override context values)
        if (pathParams != null && !pathParams.isEmpty()) {
            finalEndpoint = replacePathParameters(finalEndpoint, pathParams, shouldEncode);
        }

        // 3. Handle query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            finalEndpoint = addQueryParameters(finalEndpoint, queryParams, shouldEncode);
        }

        String fullUrl = baseUrl + finalEndpoint;
        logger.info("Built URL (encoding: {}): {}", shouldEncode, fullUrl);
        return fullUrl;
    }

    private String replacePathParameters(String endpoint, Map<String, String> pathParams, boolean shouldEncode) {
        String result = endpoint;

        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue();

            // Check if value is a context key reference (${context_key})
            if (value.startsWith("${") && value.endsWith("}")) {
                String contextKey = value.substring(2, value.length() - 1);
                value = String.valueOf(contextValues.getOrDefault(contextKey, value));
            }

            value = encodeValueIfNeeded(value, shouldEncode);
            result = result.replace(placeholder, value);
        }

        return result;
    }

    private String addQueryParameters(String endpoint, Map<String, String> queryParams, boolean shouldEncode) {
        if (queryParams.isEmpty()) return endpoint;

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!queryString.isEmpty()) {
                queryString.append("&");
            }

            String key = entry.getKey();
            String value = entry.getValue();

            // Check if value is a context key reference (${context_key})
            if (value.startsWith("${") && value.endsWith("}")) {
                String contextKey = value.substring(2, value.length() - 1);
                value = String.valueOf(contextValues.getOrDefault(contextKey, value));
            }

            key = encodeValueIfNeeded(key, shouldEncode);
            value = encodeValueIfNeeded(value, shouldEncode);

            queryString.append(key).append("=").append(value);
        }

        // Check if endpoint already has query parameters
        if (endpoint.contains("?")) {
            return endpoint + "&" + queryString;
        } else {
            return endpoint + "?" + queryString;
        }
    }

    private String replaceContextPlaceholders(String endpoint, boolean shouldEncode) {
        String result = endpoint;

        // Handle both {placeholder} and ${placeholder} patterns
        result = replacePlaceholderPattern(result, "{", shouldEncode);
        result = replacePlaceholderPattern(result, "${", shouldEncode);

        return result;
    }

    private String replacePlaceholderPattern(String text, String startDelim, boolean shouldEncode) {
        String result = text;
        Set<String> placeholders = extractPlaceholders(result, startDelim);

        for (String placeholder : placeholders) {
            String key = placeholder.substring(startDelim.length(), placeholder.length() - "}".length());

            if (contextValues.containsKey(key)) {
                String value = String.valueOf(contextValues.get(key));
                value = encodeValueIfNeeded(value, shouldEncode);
                result = result.replace(placeholder, value);
            }
        }

        return result;
    }

    private String encodeValueIfNeeded(String value, boolean shouldEncode) {
        if (!shouldEncode || isAlreadyEncoded(value)) {
            return value;
        }

        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to encode value '{}', using as-is: {}", value, e.getMessage());
            return value;
        }
    }

    private boolean isAlreadyEncoded(String value) {
        if (value == null) return false;
        // Simple heuristic: if it contains % followed by two hex digits, it's likely encoded
        return value.matches(".*%[0-9A-Fa-f]{2}.*");
    }

    public void clearRequestSpecificSettings() {
        this.requestSpecificUrlEncoding = null;
        // Clear other request-specific settings if any
    }

    private Set<String> extractPlaceholders(String text, String startDelimiter) {
        Set<String> placeholders = new HashSet<>();

        int startIndex = 0;
        while ((startIndex = text.indexOf(startDelimiter, startIndex)) != -1) {
            int endIndex = text.indexOf("}", startIndex + startDelimiter.length());
            if (endIndex == -1) break;

            String placeholder = text.substring(startIndex, endIndex + "}".length());
            placeholders.add(placeholder);
            startIndex = endIndex + "}".length();
        }

        return placeholders;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getSafeMapFromContext(String key) {
        Object value = getContextValue(key);
        if (value instanceof Map) {
            try {
                return (Map<String, String>) value;
            } catch (ClassCastException e) {
                logger.warn("Context value for '{}' is not a Map<String, String>", key);
                return null;
            }
        }
        return null;
    }

    public String resolvePlaceholders(String text) {
        if (text == null) return null;

        String result = text;
        logger.debug("Resolving placeholders in text: {}", text);

        // Extract only the placeholders that actually exist in the text
        Set<String> placeholders = extractPlaceholders(result, "${");

        for (String placeholder : placeholders) {
            String key = placeholder.substring(2, placeholder.length() - 1);

            if (contextValues.containsKey(key)) {
                Object value = contextValues.get(key);
                if (value != null) {
                    String valueStr = value.toString();
                    result = result.replace(placeholder, valueStr);
                    logger.debug("Replaced placeholder '{}' with value", placeholder);
                }
            }
        }

        logger.debug("Resolved text: {}", result);
        return result;
    }
}