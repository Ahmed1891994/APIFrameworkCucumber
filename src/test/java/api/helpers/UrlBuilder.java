package api.helpers;

import api.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UrlBuilder {
    private static final Logger logger = LoggerFactory.getLogger(UrlBuilder.class);
    private final RequestDataStore dataStore;
    private final Configuration configuration;
    private Boolean requestSpecificUrlEncoding = null;

    public UrlBuilder(RequestDataStore dataStore, Configuration configuration) {
        this.dataStore = dataStore;
        this.configuration = configuration;
    }

    public void setUrlEncodingForNextRequest(boolean enabled) {
        this.requestSpecificUrlEncoding = enabled;
        logger.debug("URL encoding for next request set to: {}", enabled);
    }

    public void clearRequestSpecificSettings() {
        this.requestSpecificUrlEncoding = null;
    }

    public String buildUrl(String baseUrl, String endpoint,
                           Map<String, String> pathParams,
                           Map<String, String> queryParams,
                           boolean encodeUrlParam) {

        boolean shouldEncode = shouldEncodeUrl(encodeUrlParam);
        logger.debug("URL encoding - Global: {}, Request-specific: {}, Param: {}, Final: {}",
                configuration.isUrlEncodingEnabled(), requestSpecificUrlEncoding,
                encodeUrlParam, shouldEncode);

        String finalEndpoint = endpoint;
        logger.debug("Building URL from base: {}, endpoint: {}", baseUrl, endpoint);

        // 1. Handle context value placeholders
        finalEndpoint = replaceContextPlaceholders(finalEndpoint, shouldEncode);

        // 2. Handle explicit path parameters
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

    private boolean shouldEncodeUrl(boolean methodParameter) {
        if (!methodParameter) return false;
        if (requestSpecificUrlEncoding != null) return requestSpecificUrlEncoding;
        return configuration.isUrlEncodingEnabled();
    }

    private String replacePathParameters(String endpoint, Map<String, String> pathParams, boolean shouldEncode) {
        String result = endpoint;

        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = resolveValueFromContext(entry.getValue());
            value = encodeValueIfNeeded(value, shouldEncode);
            result = result.replace(placeholder, value);
        }

        return result;
    }

    private String addQueryParameters(String endpoint, Map<String, String> queryParams, boolean shouldEncode) {
        if (queryParams.isEmpty()) return endpoint;

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!queryString.isEmpty()) queryString.append("&");

            String key = encodeValueIfNeeded(entry.getKey(), shouldEncode);
            String value = encodeValueIfNeeded(resolveValueFromContext(entry.getValue()), shouldEncode);
            queryString.append(key).append("=").append(value);
        }

        return endpoint + (endpoint.contains("?") ? "&" : "?") + queryString;
    }

    private String replaceContextPlaceholders(String endpoint, boolean shouldEncode) {
        String result = endpoint;
        result = replacePlaceholderPattern(result, "{", shouldEncode);
        result = replacePlaceholderPattern(result, "${", shouldEncode);
        return result;
    }

    private String replacePlaceholderPattern(String text, String startDelim, boolean shouldEncode) {
        String result = text;
        Set<String> placeholders = extractPlaceholders(result, startDelim);

        for (String placeholder : placeholders) {
            String key = placeholder.substring(startDelim.length(), placeholder.length() - 1);
            Object value = dataStore.getContextValue(key);

            if (value != null) {
                String encodedValue = encodeValueIfNeeded(value.toString(), shouldEncode);
                result = result.replace(placeholder, encodedValue);
            }
        }

        return result;
    }

    private String resolveValueFromContext(String value) {
        if (value != null && value.startsWith("${") && value.endsWith("}")) {
            String contextKey = value.substring(2, value.length() - 1);
            Object contextValue = dataStore.getContextValue(contextKey);
            return contextValue != null ? contextValue.toString() : value;
        }
        return value;
    }

    private String encodeValueIfNeeded(String value, boolean shouldEncode) {
        if (!shouldEncode || isAlreadyEncoded(value)) return value;

        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to encode value '{}', using as-is: {}", value, e.getMessage());
            return value;
        }
    }

    private boolean isAlreadyEncoded(String value) {
        return value != null && value.matches(".*%[0-9A-Fa-f]{2}.*");
    }

    private Set<String> extractPlaceholders(String text, String startDelimiter) {
        Set<String> placeholders = new HashSet<>();
        int startIndex = 0;

        while ((startIndex = text.indexOf(startDelimiter, startIndex)) != -1) {
            int endIndex = text.indexOf("}", startIndex + startDelimiter.length());
            if (endIndex == -1) break;

            String placeholder = text.substring(startIndex, endIndex + 1);
            placeholders.add(placeholder);
            startIndex = endIndex + 1;
        }

        return placeholders;
    }
}