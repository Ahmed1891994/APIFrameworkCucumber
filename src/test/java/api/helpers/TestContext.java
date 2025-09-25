package api.helpers;

import api.client.AsyncRestClient;
import api.client.SchemaValidator;
import api.config.Configuration;
import api.data.DataDrivenTestGenerator;
import api.auth.AuthManager;
import api.performance.PerformanceMonitor;

public class TestContext {
    private final Configuration configuration;
    private final AsyncRestClient restClient;
    private final SchemaValidator schemaValidator;
    private final PathExtractor pathExtractor;
    private final RegexGenerator regexGenerator;
    private final DataDrivenTestGenerator dataDrivenTestGenerator;
    private final AuthManager authManager;
    private final PerformanceMonitor performanceMonitor;
    private final OutputFileHelper outputFileHelper;
    private final RequestDataStore requestDataStore;

    public TestContext(String environment) {
        this.configuration = new Configuration(environment);
        this.performanceMonitor = new PerformanceMonitor();
        this.restClient = new AsyncRestClient(this.performanceMonitor,this.configuration);
        this.schemaValidator = new SchemaValidator();
        this.pathExtractor = new PathExtractor();
        this.regexGenerator = new RegexGenerator();
        this.dataDrivenTestGenerator = new DataDrivenTestGenerator();
        this.authManager = new AuthManager();
        this.requestDataStore = new RequestDataStore(this.configuration);
        this.outputFileHelper = new OutputFileHelper(this.requestDataStore);
    }

    // Getters for components
    public Configuration getConfiguration() { return configuration; }
    public AsyncRestClient getRestClient() { return restClient; }
    public SchemaValidator getSchemaValidator() { return schemaValidator; }
    public PathExtractor getPathExtractor() { return pathExtractor; }
    public RegexGenerator getRegexGenerator() { return regexGenerator; }
    public DataDrivenTestGenerator getDataDrivenTestGenerator() { return dataDrivenTestGenerator; }
    public AuthManager getAuthManager() { return authManager; }
    public PerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    public OutputFileHelper getFileHelper() { return outputFileHelper; }
    public RequestDataStore getRequestData() { return requestDataStore; }

    public void reset() {
        this.requestDataStore.clearAll();
        this.schemaValidator.clearCache();
    }
}