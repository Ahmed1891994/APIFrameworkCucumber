package api.definitions;

import api.helpers.TestContext;
import api.hooks.Hooks;
import api.client.AsyncRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.cucumber.java.en.*;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.testng.Assert;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class StepsDefinition {
    private static final Logger logger = LoggerFactory.getLogger(StepsDefinition.class);
    private final TestContext context;
    private final AsyncRestClient client;
    private Map<String, String> headers;
    private String endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ObjectNode requestBodyNode;

    public StepsDefinition(Hooks hooks) {
        logger.info("Initializing StepsDefinition");
        this.context = hooks.getContext();
        this.client = context.getRestClient();
        this.headers = new HashMap<>();
        this.requestBodyNode = objectMapper.createObjectNode();
        logger.debug("StepsDefinition initialized with empty headers and request body");
    }

    @Given("set base url to {string}")
    public void setBaseUrl(String baseUrl) {
        logger.info("Setting base URL to: {}", baseUrl);
        client.setBaseUrl(baseUrl);
    }

    @Given("add to headers")
    public void addHeaders(Map<String, String> headerMap) {
        logger.info("Adding headers: {}", headerMap);
        headers.putAll(headerMap);
        headerMap.forEach((key, value) -> client.addHeader(key, value));
    }

    @Given("set content type to {string}")
    public void setContentType(String contentType) {
        logger.info("Setting Content-Type: {}", contentType);
        headers.put("Content-Type", contentType);
    }

    @Given("remove content type")
    public void removeContentType() {
        logger.info("Removing Content-Type header");
        headers.remove("Content-Type");
    }

    @Given("set endpoint to {string}")
    public void setEndpoint(String path) {
        logger.info("Setting endpoint to: {}", path);
        this.endpoint = path;
    }

    @Given("generate random values")
    public void generateRandomValues(Map<String, String> patterns) {
        logger.info("Generating random values for patterns: {}", patterns);
        patterns.forEach((key, pattern) -> {
            String value = context.getRegexGenerator().generateValue(pattern);
            logger.debug("Generated value for {}: {}", key, value);
            context.getPathExtractor().extractValue("{\"" + key + "\":\"" + value + "\"}", "$." + key, key);
        });
    }

    @Given("add to body from context")
    public void addToBodyFromContext(Map<String, String> bodyParams) {
        logger.info("Adding body parameters from context: {}", bodyParams);
        bodyParams.forEach((key, contextKey) -> {
            Object value = context.getPathExtractor().getValue(contextKey);
            if (value != null) {
                requestBodyNode.put(key, value.toString());
                logger.debug("Added body parameter from context - {}: {}", key, value);
            } else {
                logger.warn("No value found in context for key: {}", contextKey);
            }
        });
    }

    @Given("add to body")
    public void addToBody(Map<String, String> bodyParams) {
        logger.info("Adding direct body parameters: {}", bodyParams);
        bodyParams.forEach((key, value) -> {
            requestBodyNode.put(key, value);
            logger.debug("Added direct body parameter - {}: {}", key, value);
        });
    }

    @When("send a {word} request")
    public void sendRequest(String method) throws Exception {
        String requestBody = requestBodyNode.toString();
        logger.info("Sending {} request", method);
        logger.debug("Request details - Endpoint: {}, Body: {}, Headers: {}", endpoint, requestBody, headers);
        CompletableFuture<SimpleHttpResponse> future;
        switch (method.toUpperCase()) {
            case "GET":
                future = client.get(endpoint, headers);
                break;
            case "POST":
                future = client.post(endpoint, requestBody, headers);
                break;
            case "PUT":
                future = client.put(endpoint, requestBody, headers);
                break;
            case "DELETE":
                future = client.delete(endpoint, headers);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        SimpleHttpResponse response = future.get(10, TimeUnit.SECONDS);

        // Store the response as a string, not a LinkedHashMap
        String responseBody = response.getBodyText();
        context.getPathExtractor().storeValue("response", responseBody);
        context.getPathExtractor().storeValue("status_code", response.getCode());

        logger.debug("Received response: {}", responseBody);
        logger.debug("Status code: {}", response.getCode());

        // Reset body for next request
        requestBodyNode = objectMapper.createObjectNode();
    }

    @Then("validate status code of {int}")
    public void validateStatusCode(int expectedStatus) {
        Integer actualStatusCode = (Integer) context.getPathExtractor().getValue("status_code");
        Assert.assertEquals(actualStatusCode, expectedStatus, "Status code mismatch");
    }

    @Then("extract values from response")
    public void extractValuesFromResponse(Map<String, String> extractions) {
        String responseStr = (String) context.getPathExtractor().getValue("response");
        extractions.forEach((jsonPath, key) -> {
            context.getPathExtractor().extractValue(responseStr, jsonPath, key);
        });
    }

    @Then("verify response schema {string}")
    public void verifyResponseSchema(String schemaName) throws IOException {
        logger.info("Validating response against schema: {}", schemaName);
        String responseBody = (String) context.getPathExtractor().getValue("response");
        logger.debug("Response body to validate: {}", responseBody);
        String schemaPath = "schemas/" + schemaName + ".json";
        context.getSchemaValidator().validateSchema(responseBody, schemaPath);
        logger.info("Schema validation successful");
    }

    @Given("add to path parameters from context")
    public void addPathParametersFromContext(Map<String, String> pathParams) {
        endpoint = context.getPathExtractor().buildUrl("", endpoint, pathParams);
    }
}