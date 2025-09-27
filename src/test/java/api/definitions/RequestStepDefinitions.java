package api.definitions;

import api.helpers.TestContext;
import api.hooks.Hooks;
import api.client.AsyncRestClient;
import io.cucumber.java.en.When;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RequestStepDefinitions {
    private final TestContext context;
    private final AsyncRestClient client;

    public RequestStepDefinitions(Hooks hooks) {
        this.context = hooks.getContext();
        this.client = context.getRestClient();
    }

    @When("send a {word} request")
    public void sendRequest(String method) throws Exception {
        sendRequestWithRetry(method, 0);
    }

    @When("send a {word} request with {int} retries")
    public void sendRequestWithRetry(String method, int retries) throws Exception {
        sendRequestWithBodyAndRetry(method, null, retries);
    }

    @When("send a {word} request with body {string}")
    public void sendRequestWithBody(String method, String bodyName) throws Exception {
        sendRequestWithBodyAndRetry(method, bodyName, 0);
    }

    @When("send a {word} request with body {string} and {int} retries")
    public void sendRequestWithBodyAndRetry(String method, String bodyName, int retries) throws Exception {
        Map<String, String> headers = null;
        try {
            String endpoint = context.getRequestData().getEndpoint();

            // Safe casting with type checking
            Map<String, String> pathParams = context.getRequestData().getSafeMapFromContext("path_params");
            Map<String, String> queryParams = context.getRequestData().getSafeMapFromContext("query_params");

            String finalEndpoint = context.getUrlBuilder().buildUrl(
                    context.getConfiguration().getBaseUrl(),
                    endpoint,
                    pathParams != null ? pathParams : Collections.emptyMap(),
                    queryParams != null ? queryParams : Collections.emptyMap(),
                    true  // Method-level encoding control
            );
            // Apply headers from the last used header set
            String lastHeaderSet = context.getRequestData().getLastHeaderSet();
            if (lastHeaderSet != null) {
                headers = context.getRequestData().getHeaders(lastHeaderSet);
            }

            // Determine request body using RequestDataStore
            String requestBody = null;
            if (bodyName != null) {
                requestBody = context.getRequestData().getJsonBody(bodyName);
            } else {
                String lastBodySet = context.getRequestData().getLastBodySet();
                if (lastBodySet != null) {
                    requestBody = context.getRequestData().getJsonBody(lastBodySet);
                }
            }

            // Handle GET/DELETE methods - should not have body
            if (("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) && requestBody != null) {
                requestBody = null;
            }

            context.getRequestData().storeContextValue("last_request_endpoint", finalEndpoint);

            CompletableFuture<SimpleHttpResponse> future = switch (method.toUpperCase()) {
                case "GET" -> client.sendRequest("GET", finalEndpoint, null, headers, retries);
                case "POST" -> client.sendRequest("POST", finalEndpoint, requestBody, headers, retries);
                case "PUT" -> client.sendRequest("PUT", finalEndpoint, requestBody, headers, retries);
                case "PATCH" -> client.sendRequest("PATCH", finalEndpoint, requestBody, headers, retries);
                case "DELETE" -> client.sendRequest("DELETE", finalEndpoint, null, headers, retries);
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };

            SimpleHttpResponse response = future.get(30, TimeUnit.SECONDS);

            // Store response data
            String responseBody = response.getBody() != null ? response.getBodyText() : "";

            // Store response data
            context.getRequestData().storeContextValue("response", responseBody);
            context.getRequestData().storeContextValue("status_code", String.valueOf(response.getCode()));
            context.getRequestData().storeContextValue("last_request_endpoint", finalEndpoint);

            // Store response headers
            Map<String, String> responseHeadersMap = context.getRequestData().convertHeadersToMap(response.getHeaders());
            context.getRequestData().storeResponseHeaders(responseHeadersMap);

        } finally {
            context.getUrlBuilder().clearRequestSpecificSettings();
        }
    }
}