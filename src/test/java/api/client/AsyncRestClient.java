package api.client;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AsyncRestClient {
    private static final Logger logger = LoggerFactory.getLogger(AsyncRestClient.class);
    private final CloseableHttpAsyncClient client;
    private final Map<String, String> defaultHeaders;
    private String baseUrl;
    private SimpleHttpRequest lastRequest;
    private SimpleHttpResponse lastResponse;

    // Interceptor interfaces
    public interface RequestInterceptor {
        void intercept(SimpleHttpRequest request);
    }

    public interface ResponseInterceptor {
        void intercept(SimpleHttpResponse response);
    }

    private final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
    private final List<ResponseInterceptor> responseInterceptors = new ArrayList<>();

    public AsyncRestClient() {
        this(HttpAsyncClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                        .setResponseTimeout(Timeout.ofSeconds(30))
                        .build())
                .build());
    }

    public AsyncRestClient(CloseableHttpAsyncClient customClient) {
        logger.info("Initializing AsyncRestClient");
        this.client = customClient;
        this.client.start();
        this.defaultHeaders = new HashMap<>();
        logger.info("AsyncRestClient initialized with default headers: {}", defaultHeaders);
    }

    public void setBaseUrl(String baseUrl) {
        logger.info("Setting base URL to: {}", baseUrl);
        this.baseUrl = baseUrl;
    }

    public void addHeader(String key, String value) {
        logger.info("Adding header: {} = {}", key, value);
        this.defaultHeaders.put(key, value);
    }

    public Map<String, String> getDefaultHeaders() {
        return new HashMap<>(defaultHeaders);
    }

    public void addRequestInterceptor(RequestInterceptor interceptor) {
        requestInterceptors.add(interceptor);
    }

    public void addResponseInterceptor(ResponseInterceptor interceptor) {
        responseInterceptors.add(interceptor);
    }

    public SimpleHttpRequest getLastRequest() {
        return lastRequest;
    }

    public SimpleHttpResponse getLastResponse() {
        return lastResponse;
    }

    public CompletableFuture<SimpleHttpResponse> sendRequest(String method, String endpoint, String body, Map<String, String> headers) {
        return sendRequest(method, endpoint, body, headers, 0);
    }

    public CompletableFuture<SimpleHttpResponse> sendRequest(String method, String endpoint, String body, Map<String, String> headers, int maxRetries) {
        String url = baseUrl + endpoint;

        // Auto-manage Content-Type
        Map<String, String> finalHeaders = new HashMap<>(defaultHeaders);
        if (headers != null) {
            finalHeaders.putAll(headers);
        }

        // Remove Content-Type header if body is empty/null
        if (body == null || body.isBlank()) {
            finalHeaders.remove("Content-Type");
        }

        SimpleHttpRequest request;
        if (body != null && !body.isEmpty() && (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH"))) {
            request = SimpleRequestBuilder.create(method)
                    .setUri(url)
                    .setBody(body, ContentType.APPLICATION_JSON)
                    .build();
        } else {
            request = SimpleRequestBuilder.create(method)
                    .setUri(url)
                    .build();
        }

        // Apply headers - this will override any Content-Type set by setBody
        finalHeaders.forEach(request::addHeader);

        // Apply request interceptors
        for (RequestInterceptor interceptor : requestInterceptors) {
            interceptor.intercept(request);
        }


        if (body == null || Objects.requireNonNull(body).isEmpty()) {
            logger.info("Request body: {}", body);
        }

        // Store the request
        this.lastRequest = request;

        CompletableFuture<SimpleHttpResponse> responseFuture = new CompletableFuture<>();

        logger.info("🚀 Sending {} request to: {}", method, url);
        logger.info("Request body: {}", body);
        logger.info("Request headers: {}", finalHeaders);

        // Execute with retry logic
        executeWithRetry(request, responseFuture, maxRetries, 0);

        return responseFuture;
    }

    private void executeWithRetry(SimpleHttpRequest request, CompletableFuture<SimpleHttpResponse> responseFuture, int maxRetries, int attempt) {
        client.execute(
                request,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.info("✅ Request completed with status: {}", result.getCode());
                        logger.info("Response body: {}", result.getBodyText());

                        // Store the response
                        lastResponse = result;

                        // Apply response interceptors
                        for (ResponseInterceptor interceptor : responseInterceptors) {
                            interceptor.intercept(result);
                        }

                        responseFuture.complete(result);
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("❌ Request failed: {}", ex.getMessage());

                        if (attempt < maxRetries) {
                            logger.info("Retrying request (attempt {}/{})", attempt + 1, maxRetries);
                            try {
                                Thread.sleep(1000 * (long) Math.pow(2, attempt)); // Exponential backoff
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            executeWithRetry(request, responseFuture, maxRetries, attempt + 1);
                        } else {
                            responseFuture.completeExceptionally(ex);
                        }
                    }

                    @Override
                    public void cancelled() {
                        logger.warn("Request was cancelled");
                        responseFuture.cancel(true);
                    }
                });
    }

    public String requestToString(SimpleHttpRequest request) throws URISyntaxException {
        if (request == null) return "No request made";

        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append(" ").append(request.getUri()).append("\n");
        Arrays.stream(request.getHeaders()).forEach(h ->
                sb.append(h.getName()).append(": ").append(h.getValue()).append("\n"));
        if (request.getBody() != null) {
            sb.append("\n").append(request.getBodyText());
        }
        return sb.toString();
    }

    public String responseToString(SimpleHttpResponse response) {
        if (response == null) return "No response received";

        StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(response.getCode()).append("\n");
        Arrays.stream(response.getHeaders()).forEach(h ->
                sb.append(h.getName()).append(": ").append(h.getValue()).append("\n"));
        if (response.getBody() != null) {
            sb.append("\n").append(response.getBodyText());
        }
        return sb.toString();
    }

    public CompletableFuture<SimpleHttpResponse> get(String endpoint, Map<String, String> headers) {
        return sendRequest("GET", endpoint, null, headers);
    }

    public CompletableFuture<SimpleHttpResponse> get(String endpoint, Map<String, String> headers, int maxRetries) {
        return sendRequest("GET", endpoint, null, headers, maxRetries);
    }

    public CompletableFuture<SimpleHttpResponse> post(String endpoint, String body, Map<String, String> headers) {
        return sendRequest("POST", endpoint, body, headers);
    }

    public CompletableFuture<SimpleHttpResponse> post(String endpoint, String body, Map<String, String> headers, int maxRetries) {
        return sendRequest("POST", endpoint, body, headers, maxRetries);
    }

    public CompletableFuture<SimpleHttpResponse> put(String endpoint, String body, Map<String, String> headers) {
        return sendRequest("PUT", endpoint, body, headers);
    }

    public CompletableFuture<SimpleHttpResponse> put(String endpoint, String body, Map<String, String> headers, int maxRetries) {
        return sendRequest("PUT", endpoint, body, headers, maxRetries);
    }

    public CompletableFuture<SimpleHttpResponse> patch(String endpoint, String body, Map<String, String> headers) {
        return sendRequest("PATCH", endpoint, body, headers);
    }

    public CompletableFuture<SimpleHttpResponse> patch(String endpoint, String body, Map<String, String> headers, int maxRetries) {
        return sendRequest("PATCH", endpoint, body, headers, maxRetries);
    }

    public CompletableFuture<SimpleHttpResponse> delete(String endpoint, Map<String, String> headers) {
        return sendRequest("DELETE", endpoint, null, headers);
    }

    public CompletableFuture<SimpleHttpResponse> delete(String endpoint, Map<String, String> headers, int maxRetries) {
        return sendRequest("DELETE", endpoint, null, headers, maxRetries);
    }

    public void close() {
        logger.info("Closing AsyncRestClient");
        try {
            client.close();
        } catch (Exception e) {
            logger.error("Error closing AsyncRestClient", e);
        }
    }
}