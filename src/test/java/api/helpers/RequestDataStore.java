package api.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RequestDataStore {
    private static final Logger logger = LoggerFactory.getLogger(RequestDataStore.class);

    private final Map<String, Map<String, String>> requestHeadersMap = new HashMap<>();
    private final Map<String, ObjectNode> requestJsonBodyMap = new ConcurrentHashMap<>();
    private final Map<String, Object> contextValues = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String lastHeaderSet;
    private String lastBodySet;
    private String endpoint;

    public RequestDataStore() {
        initializeDefaults();
        logger.debug("Initialized RequestDataStore");
    }

    public void clearAll() {
        int headersCount = requestHeadersMap.size();
        int bodiesCount = requestJsonBodyMap.size();
        int contextCount = contextValues.size();

        requestHeadersMap.clear();
        requestJsonBodyMap.clear();
        contextValues.clear();

        endpoint = null;

        initializeDefaults();

        logger.info("Cleared all request data: {} header sets, {} bodies, {} context values",
                headersCount, bodiesCount, contextCount);
    }

    // Header management methods
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

        if (storedMap == null) {
            logger.debug("Retrieved headers from set '{}': 0 headers", headerSetName);
            return new HashMap<>();
        }

        Map<String, String> resolvedHeaders = new HashMap<>();
        for (Map.Entry<String, String> entry : storedMap.entrySet()) {
            String resolvedValue = resolvePlaceholders(entry.getValue());
            resolvedHeaders.put(entry.getKey(), resolvedValue);
        }

        logger.debug("Retrieved headers from set '{}': {} headers", headerSetName, resolvedHeaders.size());
        return resolvedHeaders;
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

    // Add these methods to your RequestDataStore class

    /**
     * Store response headers in context for later use
     */
    public void storeResponseHeaders(Map<String, String> responseHeaders) {
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            contextValues.put("response_headers", new HashMap<>(responseHeaders));
            logger.debug("Stored {} response headers in context", responseHeaders.size());
        }
    }

    /**
     * Get a specific header from the stored response headers
     */
    public String getResponseHeader(String headerName) {
        @SuppressWarnings("unchecked")
        Map<String, String> responseHeaders = (Map<String, String>) contextValues.get("response_headers");
        if (responseHeaders != null) {
            return responseHeaders.get(headerName);
        }
        return null;
    }

    /**
     * Get all stored response headers
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getAllResponseHeaders() {
        Map<String, String> headers = (Map<String, String>) contextValues.get("response_headers");
        return headers != null ? new HashMap<>(headers) : new HashMap<>();
    }

    /**
     * Store a specific header value in context with a custom key
     */
    public void storeHeaderToContext(String headerName, String contextKey) {
        String headerValue = getResponseHeader(headerName);
        if (headerValue != null) {
            storeContextValue(contextKey, headerValue);
            logger.debug("Stored header '{}' value to context key '{}'", headerName, contextKey);
        } else {
            logger.warn("Header '{}' not found in response headers", headerName);
        }
    }

    // Add this helper method to convert Header[] to Map<String, String>
    public Map<String, String> convertHeadersToMap(org.apache.hc.core5.http.Header[] headers) {
        Map<String, String> headersMap = new HashMap<>();
        if (headers != null) {
            for (org.apache.hc.core5.http.Header header : headers) {
                headersMap.put(header.getName(), header.getValue());
            }
        }
        return headersMap;
    }

    // JSON body management methods
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

    public void removeKeysFromBody(String bodyName, List<String> keys) {
        ObjectNode jsonBody = requestJsonBodyMap.get(bodyName);
        if (jsonBody != null && keys != null) {
            int beforeSize = jsonBody.size();
            // You'll need to implement removeKeys method or use this:
            keys.forEach(jsonBody::remove);
            logger.debug("Removed {} keys from body '{}'", beforeSize - jsonBody.size(), bodyName);
        }
    }

    // Context value management
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

    // Getters and Setters
    public String getLastHeaderSet() { return lastHeaderSet; }
    public String getLastBodySet() { return lastBodySet; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        logger.debug("Set endpoint to: {}", endpoint);
    }

    private void initializeDefaults() {
        requestHeadersMap.put("default", new HashMap<>());
        requestJsonBodyMap.put("default", objectMapper.createObjectNode());
        lastBodySet = "default";
        lastHeaderSet = "default";
    }

    private String getHeaderDefault(String headerSetName) {
        return (headerSetName == null || headerSetName.trim().isEmpty()) ? "default" : headerSetName;
    }

    public String resolvePlaceholders(String text) {
        if (text == null) return null;

        String result = text;
        logger.debug("Resolving placeholders in text: {}", text);

        Set<String> placeholders = extractPlaceholders(result);
        for (String placeholder : placeholders) {
            String key = placeholder.substring(2, placeholder.length() - 1);
            Object value = getContextValue(key);

            if (value != null) {
                result = result.replace(placeholder, value.toString());
                logger.debug("Replaced placeholder '{}' with value", placeholder);
            }
        }

        logger.debug("Resolved text: {}", result);
        return result;
    }

    private Set<String> extractPlaceholders(String text) {
        Set<String> placeholders = new HashSet<>();
        int startIndex = 0;

        while ((startIndex = text.indexOf("${", startIndex)) != -1) {
            int endIndex = text.indexOf("}", startIndex + "${".length());
            if (endIndex == -1) break;

            String placeholder = text.substring(startIndex, endIndex + 1);
            placeholders.add(placeholder);
            startIndex = endIndex + 1;
        }
        return placeholders;
    }
}