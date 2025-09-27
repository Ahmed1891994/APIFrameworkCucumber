package api.definitions;

import api.helpers.TestContext;
import api.hooks.Hooks;
import io.cucumber.java.en.Then;
import org.testng.Assert;

import java.io.IOException;
import java.util.Map;

public class ValidationStepDefinitions {
    private final TestContext context;

    public ValidationStepDefinitions(Hooks hooks) {
        this.context = hooks.getContext();
    }

    @Then("validate status code of {int}")
    public void validateStatusCode(int expectedStatus) {
        String statusCodeStr = (String) context.getRequestData().getContextValue("status_code");
        int actualStatusCode = Integer.parseInt(statusCodeStr);
        Assert.assertEquals(actualStatusCode, expectedStatus, "Status code mismatch");
    }

    @Then("extract values from response")
    public void extractValuesFromResponse(Map<String, String> extractions) {
        String responseStr = (String) context.getRequestData().getContextValue("response");
        extractions.forEach((jsonPath, key) -> {
            Object extractedValue = context.getPathExtractor().extractValue(responseStr, jsonPath, key);
            context.getRequestData().storeContextValue(key, extractedValue);
        });
    }

    @Then("verify response contains {string}")
    public void verifyResponseContains(String expectedText) {
        String response = (String) context.getRequestData().getContextValue("response");
        String resolvedValue = context.getRequestData().resolvePlaceholders(expectedText);
        Assert.assertTrue(response.contains(resolvedValue), "Response does not contain: " + resolvedValue);
    }

    @Then("verify response json path {string} equals {string}")
    public void verifyJsonPath(String jsonPath, String expectedValue) {
        String response = (String) context.getRequestData().getContextValue("response");
        String resolvedValue = context.getRequestData().resolvePlaceholders(expectedValue);
        Object actualValue = context.getPathExtractor().readJsonPath(response, jsonPath);
        Assert.assertEquals(String.valueOf(actualValue), resolvedValue,
                "JSON path " + jsonPath + " mismatch");
    }

    @Then("verify response json path {string} equals null")
    public void verifyJsonPathEqualsNull(String jsonPath) {
        String response = (String) context.getRequestData().getContextValue("response");
        Object actualValue = context.getPathExtractor().readJsonPath(response, jsonPath);
        Assert.assertTrue(actualValue == null || "null".equals(String.valueOf(actualValue)),
                "JSON path " + jsonPath + " should be null but was: " + actualValue);
    }

    @Then("verify response json path {string} is null")
    public void verifyJsonPathIsNull(String jsonPath) {
        String response = (String) context.getRequestData().getContextValue("response");
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
        String response = (String) context.getRequestData().getContextValue("response");
        Assert.assertTrue(context.getPathExtractor().existsJsonPath(response, jsonPath),
                "JSON path " + jsonPath + " does not exist");
    }

    @Then("verify response json path {string} not exists")
    public void verifyJsonPathNotExists(String jsonPath) {
        String response = (String) context.getRequestData().getContextValue("response");
        Assert.assertFalse(context.getPathExtractor().existsJsonPath(response, jsonPath),
                "JSON path " + jsonPath + " should not exist but it does");
    }

    @Then("verify response schema {string}")
    public void verifyResponseSchema(String schemaName) throws IOException {
        String responseBody = (String) context.getRequestData().getContextValue("response");
        context.getSchemaValidator().validateSchema(responseBody, schemaName);
    }

    @Then("verify response schema from classpath {string}")
    public void verifyResponseSchemaFromClasspath(String schemaName) throws IOException {
        String responseBody = (String) context.getRequestData().getContextValue("response");
        context.getSchemaValidator().validateSchemaAgainstClasspath(responseBody, schemaName);
    }

    @Then("validate response header {string} equals {string}")
    public void validateResponseHeaderEquals(String headerName, String expectedValue) {
        String resolvedExpectedValue = context.getRequestData().resolvePlaceholders(expectedValue);
        String actualHeaderValue = context.getRequestData().getResponseHeader(headerName);

        Assert.assertNotNull(actualHeaderValue, "Response header '" + headerName + "' not found");
        Assert.assertEquals(actualHeaderValue, resolvedExpectedValue,
                "Response header '" + headerName + "' mismatch");
    }

    @Then("validate response header {string} contains {string}")
    public void validateResponseHeaderContains(String headerName, String expectedText) {
        String resolvedExpectedText = context.getRequestData().resolvePlaceholders(expectedText);
        String actualHeaderValue = context.getRequestData().getResponseHeader(headerName);

        Assert.assertNotNull(actualHeaderValue, "Response header '" + headerName + "' not found");
        Assert.assertTrue(actualHeaderValue.contains(resolvedExpectedText),
                "Response header '" + headerName + "' should contain: " + resolvedExpectedText);
    }

    @Then("validate response header {string} exists")
    public void validateResponseHeaderExists(String headerName) {
        String actualHeaderValue = context.getRequestData().getResponseHeader(headerName);
        Assert.assertNotNull(actualHeaderValue, "Response header '" + headerName + "' should exist but was not found");
    }

    @Then("validate response header {string} not exists")
    public void validateResponseHeaderNotExists(String headerName) {
        String actualHeaderValue = context.getRequestData().getResponseHeader(headerName);
        Assert.assertNull(actualHeaderValue, "Response header '" + headerName + "' should not exist but was found: " + actualHeaderValue);
    }

    @Then("save response header {string} to context as {string}")
    public void saveResponseHeaderToContext(String headerName, String contextKey) {
        context.getRequestData().storeHeaderToContext(headerName, contextKey);
    }

    @Then("save all response headers to context")
    public void saveAllResponseHeadersToContext() {
        Map<String, String> allHeaders = context.getRequestData().getAllResponseHeaders();
        context.getRequestData().storeContextValue("all_response_headers", allHeaders);
    }
}