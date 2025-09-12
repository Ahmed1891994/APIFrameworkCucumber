package api.definitions;

import api.helpers.JsonHelper;
import api.helpers.TestContext;
import api.hooks.Hooks;
import api.client.AsyncRestClient;
import api.performance.PerformanceMonitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.cucumber.java.en.*;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.testng.Assert;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class StepsDefinition {
    private static final Logger logger = LoggerFactory.getLogger(StepsDefinition.class);
    private final TestContext context;
    private final AsyncRestClient client;
    private String endpoint;
    private String authType;

    public StepsDefinition(Hooks hooks) {
        logger.info("Initializing StepsDefinition");
        this.context = hooks.getContext();
        this.client = context.getRestClient();
    }

    // Simple step definitions - most logic moved to helper classes
    @Given("set base url to {string}")
    public void setBaseUrl(String baseUrl) {
        client.setBaseUrl(baseUrl);
    }

    @Given("add to headers")
    public void addHeaders(Map<String, String> headerMap) {
        headerMap.forEach(client::addHeader);
    }

    @Given("set endpoint to {string}")
    public void setEndpoint(String path) {
        this.endpoint = path;
    }

    @Given("generate random values")
    public void generateRandomValues(Map<String, String> patterns) {
        patterns.forEach((key, pattern) -> {
            String value = context.getRegexGenerator().generateValue(pattern);
            context.getPathExtractor().storeValue(key, value);
        });
    }

    @Given("add to body from context")
    public void addToBodyFromContext(Map<String, String> bodyParams) {
        ObjectNode requestBody = context.getRequestBody(endpoint);
        bodyParams.forEach((key, contextKey) -> {
            Object value = context.getPathExtractor().getValue(contextKey);
            // Store in PathExtractor for potential future use
            context.getPathExtractor().storeValue(key, value);
            // Actually add to the request body JSON
            JsonHelper.addValue(requestBody, key, value);
        });
    }

    @Given("add to body")
    public void addToBody(Map<String, String> bodyParams) {
        ObjectNode requestBody = context.getRequestBody(endpoint);
        bodyParams.forEach((key, value) -> {
            // Store in PathExtractor for potential future use
            context.getPathExtractor().storeValue(key, value);
            // Actually add to the request body JSON
            JsonHelper.addValue(requestBody, key, value);
        });
    }

    @Given("remove from body")
    public void removeFromBody(List<String> keys) {
        context.removeFromRequestBody(endpoint, keys);
    }

    @Given("clear body for endpoint {string}")
    public void clearBodyForEndpoint(String specificEndpoint) {
        context.clearRequestBody(specificEndpoint);
    }

    @Given("clear current body")
    public void clearCurrentBody() {
        context.clearRequestBody(endpoint);
    }

    @Given("load test data from {string}")
    public void loadTestData(String dataSource) {
        List<Map<String, String>> testData = context.getDataDrivenTestGenerator().loadTestData(dataSource);
        context.setData("test_data", testData);
    }

    @Given("use test data row {int}")
    public void useTestDataRow(int rowIndex) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> testData = (List<Map<String, String>>) context.getData("test_data");

        if (testData == null || rowIndex >= testData.size()) {
            throw new RuntimeException("Test data not available or invalid row index: " + rowIndex);
        }

        Map<String, String> rowData = testData.get(rowIndex);
        rowData.forEach(context.getPathExtractor()::storeValue);
    }

    @Given("apply authentication {string}")
    public void applyAuthentication(String authType) {
        this.authType = authType;
    }

    @When("send a {word} request")
    public void sendRequest(String method) throws Exception {
        sendRequestWithRetry(method, 0);
    }

    @When("send a {word} request with {int} retries")
    public void sendRequestWithRetry(String method, int retries) throws Exception {
        String finalEndpoint = context.getPathExtractor().buildUrl("", endpoint, Collections.emptyMap());

        String requestBody = context.getRequestBody(endpoint).toString();

        if (authType != null) {
            Map<String, String> authParams = context.getApiConfig().getPropertiesWithPrefix("auth.");
            context.getAuthManager().applyAuthToClient(authType, authParams, client);
        }

        context.getPathExtractor().storeValue("last_request_endpoint", finalEndpoint);

        CompletableFuture<SimpleHttpResponse> future = switch (method.toUpperCase()) {
            case "GET" -> client.get(finalEndpoint, null, retries);
            case "POST" -> client.post(finalEndpoint, requestBody, null, retries);
            case "PUT" -> client.put(finalEndpoint, requestBody, null, retries);
            case "PATCH" -> client.patch(finalEndpoint, requestBody, null, retries);
            case "DELETE" -> client.delete(finalEndpoint, null, retries);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        SimpleHttpResponse response = future.get(30, TimeUnit.SECONDS);

        context.getPathExtractor().storeValue("response", response.getBodyText());
        context.getPathExtractor().storeValue("status_code", response.getCode());

        // ✅ You can also store it again here if you want the final URL after redirects
        context.getPathExtractor().storeValue("last_request_endpoint", finalEndpoint);
    }

    @When("send a {word} request and measure performance")
    public void sendRequestAndMeasurePerformance(String method) throws Exception {
        long startTime = System.currentTimeMillis();
        sendRequest(method);
        long endTime = System.currentTimeMillis();

        String endpoint = (String) context.getPathExtractor().getValue("last_request_endpoint");
        context.getPerformanceMonitor().recordResponseTime(endpoint, endTime - startTime);
    }

    @Then("validate status code of {int}")
    public void validateStatusCode(int expectedStatus) {
        Integer actualStatusCode = (Integer) context.getPathExtractor().getValue("status_code");
        Assert.assertEquals(actualStatusCode, expectedStatus, "Status code mismatch");
    }

    @Then("extract values from response")
    public void extractValuesFromResponse(Map<String, String> extractions) {
        String responseStr = (String) context.getPathExtractor().getValue("response");
        extractions.forEach((jsonPath, key) ->
                context.getPathExtractor().extractValue(responseStr, jsonPath, key));
    }

    @Then("verify response schema {string}")
    public void verifyResponseSchema(String schemaName) throws IOException {
        String responseBody = (String) context.getPathExtractor().getValue("response");
        context.getSchemaValidator().validateSchema(responseBody, schemaName);
    }

    @Then("verify response schema from classpath {string}")
    public void verifyResponseSchemaFromClasspath(String schemaName) throws IOException {
        String responseBody = (String) context.getPathExtractor().getValue("response");
        context.getSchemaValidator().validateSchemaAgainstClasspath(responseBody, schemaName);
    }

    @Given("add to path parameters from context")
    public void addPathParametersFromContext(Map<String, String> pathParams) {
        endpoint = context.getPathExtractor().buildUrl("", endpoint, pathParams);
    }

    @Then("verify response contains {string}")
    public void verifyResponseContains(String expectedText) {
        String response = (String) context.getPathExtractor().getValue("response");
        String resolvedValue = context.getPathExtractor().resolvePlaceholders(expectedText);
        Assert.assertTrue(response.contains(resolvedValue), "Response does not contain: " + resolvedValue);
    }

    @Then("verify response json path {string} equals {string}")
    public void verifyJsonPath(String jsonPath, String expectedValue) {
        String response = (String) context.getPathExtractor().getValue("response");
        String resolvedValue = context.getPathExtractor().resolvePlaceholders(expectedValue);

        Object actualValue = context.getPathExtractor().readJsonPath(response, jsonPath);
        Assert.assertEquals(String.valueOf(actualValue), resolvedValue,
                "JSON path " + jsonPath + " mismatch");
    }

    @Then("verify response json path {string} equals null")
    public void verifyJsonPathEqualsNull(String jsonPath) {
        String response = (String) context.getPathExtractor().getValue("response");
        Object actualValue = context.getPathExtractor().readJsonPath(response, jsonPath);
        Assert.assertTrue(actualValue == null || "null".equals(String.valueOf(actualValue)),
                "JSON path " + jsonPath + " should be null but was: " + actualValue);
    }

    @Then("verify response json path {string} is null")
    public void verifyJsonPathIsNull(String jsonPath) {
        String response = (String) context.getPathExtractor().getValue("response");
        Object actualValue = context.getPathExtractor().readJsonPath(response, jsonPath);

        if (actualValue == null) {
            boolean pathExists = context.getPathExtractor().existsJsonPath(response, jsonPath);
            Assert.assertTrue(pathExists, "JSON path " + jsonPath + " does not exist");
        } else {
            Assert.fail("JSON path " + jsonPath + " should be null but was: " + actualValue);
        }
    }

    @Then("verify response json path {string} exists")
    public void verifyJsonPathExists(String jsonPath) {
        String response = (String) context.getPathExtractor().getValue("response");
        Assert.assertTrue(context.getPathExtractor().existsJsonPath(response, jsonPath),
                "JSON path " + jsonPath + " does not exist");
    }

    @Then("verify response json path {string} not exists")
    public void verifyJsonPathNotExists(String jsonPath) {
        String response = (String) context.getPathExtractor().getValue("response");
        Assert.assertFalse(context.getPathExtractor().existsJsonPath(response, jsonPath),
                "JSON path " + jsonPath + " should not exist but it does");
    }

    @Then("save response to file {string}")
    public void saveResponseToFile(String filename) throws IOException {
        context.getFileHelper().saveResponseToFile(filename, false);
    }

    @Then("append response to file {string}")
    public void appendResponseToFile(String filename) throws IOException {
        context.getFileHelper().saveResponseToFile(filename, true);
    }

    @Then("wait for {int} seconds")
    public void waitForSeconds(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
    }

    @Then("verify maximum response time for {string} is less than {long} ms")
    public void verifyMaxResponseTime(String endpoint, long maxAllowedMs) {
        PerformanceMonitor.PerformanceReport report = context.getPerformanceMonitor().generateReport();
        report.assertMaxResponseTime(endpoint, maxAllowedMs);
    }
}