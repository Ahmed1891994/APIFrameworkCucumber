package api.helpers;

import api.client.AsyncRestClient;
import api.client.SchemaValidator;

public class TestContext {
    private AsyncRestClient restClient;
    private SchemaValidator schemaValidator;
    private PathExtractor pathExtractor;
    private RegexGenerator regexGenerator;

    public TestContext() {
        this.restClient = new AsyncRestClient();
        this.schemaValidator = new SchemaValidator();
        this.pathExtractor = new PathExtractor();
        this.regexGenerator = new RegexGenerator();
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

    public void reset() {
        this.pathExtractor.clear();
    }
}