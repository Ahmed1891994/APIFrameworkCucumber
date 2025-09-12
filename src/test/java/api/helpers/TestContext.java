package api.helpers;

import api.client.AsyncRestClient;
import api.client.SchemaValidator;
import api.config.ApiConfig;
import api.data.DataDrivenTestGenerator;
import api.auth.AuthManager;
import api.performance.PerformanceMonitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestContext {
    private final ApiConfig apiConfig;
    private final AsyncRestClient restClient;
    private final SchemaValidator schemaValidator;
    private final PathExtractor pathExtractor;
    private final RegexGenerator regexGenerator;
    private final DataDrivenTestGenerator dataDrivenTestGenerator;
    private final AuthManager authManager;
    private final PerformanceMonitor performanceMonitor;
    private final Map<String, Object> scenarioData;
    private final Map<String, ObjectNode> requestBodies = new ConcurrentHashMap<>();
    private String currentRequestBodyKey = "default";
    private final FileHelper fileHelper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestContext() {
        this(System.getProperty("env", "dev")); // Default to dev environment
    }

    public TestContext(String environment) {
        this.apiConfig = new ApiConfig(environment);
        this.restClient = new AsyncRestClient();
        this.schemaValidator = new SchemaValidator();
        this.pathExtractor = new PathExtractor();
        this.regexGenerator = new RegexGenerator();
        this.dataDrivenTestGenerator = new DataDrivenTestGenerator();
        this.authManager = new AuthManager();
        this.performanceMonitor = new PerformanceMonitor();
        this.scenarioData = new ConcurrentHashMap<>();
        this.fileHelper = new FileHelper(this.pathExtractor);

        // Apply configuration to rest client
        this.restClient.setBaseUrl(apiConfig.getBaseUrl());
    }

    // Getters for all components
    public ApiConfig getApiConfig() {
        return apiConfig;
    }

    public AsyncRestClient getRestClient() {
        return restClient;
    }

    public SchemaValidator getSchemaValidator() {
        return schemaValidator;
    }

    public PathExtractor getPathExtractor() {
        return pathExtractor;
    }

    public RegexGenerator getRegexGenerator() {
        return regexGenerator;
    }

    public DataDrivenTestGenerator getDataDrivenTestGenerator() {
        return dataDrivenTestGenerator;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public void setData(String key, Object value) {
        scenarioData.put(key, value);
    }

    public Object getData(String key) {
        return scenarioData.get(key);
    }

    public boolean hasData(String key) {
        return scenarioData.containsKey(key);
    }

    public ObjectNode getRequestBody() {
        return getRequestBody("default");
    }

    public ObjectNode getRequestBody(String endpoint) {
        String bodyKey = generateBodyKeyFromEndpoint(endpoint);
        return requestBodies.computeIfAbsent(bodyKey,
                k -> objectMapper.createObjectNode());
    }

    public FileHelper getFileHelper() {
        return fileHelper;
    }

    public void clearRequestBody() {
        clearRequestBody("default");
    }

    public void clearRequestBody(String endpoint) {
        String bodyKey = generateBodyKeyFromEndpoint(endpoint);
        requestBodies.put(bodyKey, objectMapper.createObjectNode());
    }

    public void removeFromRequestBody(String endpoint, List<String> keys) {
        ObjectNode body = getRequestBody(endpoint);
        keys.forEach(key -> {
            if (body.has(key)) {
                body.remove(key);
            }
        });
    }

    private String generateBodyKeyFromEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return "default";
        }
        return endpoint.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }

    public void reset() {
        this.pathExtractor.clear();
        this.scenarioData.clear();
        this.schemaValidator.clearCache();
    }
}