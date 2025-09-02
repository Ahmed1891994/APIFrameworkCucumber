package api.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.concurrent.FutureCallback;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class AsyncRestClient {
    private static final Logger logger = LoggerFactory.getLogger(AsyncRestClient.class);
    private final CloseableHttpAsyncClient client;
    private final Map<String, String> defaultHeaders;
    private String baseUrl;

    public AsyncRestClient() {
        logger.info("Initializing AsyncRestClient");
        this.client = HttpAsyncClients.createDefault();
        this.client.start();
        this.defaultHeaders = new HashMap<>();
        this.defaultHeaders.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        logger.debug("AsyncRestClient initialized with default content type: {}", ContentType.APPLICATION_JSON.getMimeType());
    }

    public void setBaseUrl(String baseUrl) {
        logger.info("Setting base URL to: {}", baseUrl);
        this.baseUrl = baseUrl;
    }

    public void addHeader(String key, String value) {
        logger.debug("Adding header: {} = {}", key, value);
        this.defaultHeaders.put(key, value);
    }

    public CompletableFuture<SimpleHttpResponse> sendRequest(String method, String endpoint, String body, Map<String, String> headers) {
        String url = baseUrl + endpoint;

        logger.info("Sending {} request to: {}", method, url);
        logger.debug("Request body: {}", body);
        logger.debug("Request headers: {}", headers);

        // Auto-manage Content-Type
        Map<String, String> finalHeaders = new HashMap<>();
        if (headers != null) {
            finalHeaders.putAll(headers);
        }

        if (body != null && !body.isEmpty() && !finalHeaders.containsKey("Content-Type")) {
            finalHeaders.put("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        }
        else if (body == null || body.isEmpty()) {
            finalHeaders.remove("Content-Type");
        }

        SimpleHttpRequest request;
        if (body != null && !body.isEmpty() && (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT"))) {
            request = SimpleRequestBuilder.create(method)
                    .setUri(url)
                    .setBody(body, ContentType.APPLICATION_JSON)
                    .build();
        } else {
            request = SimpleRequestBuilder.create(method)
                    .setUri(url)
                    .build();
        }

        // Apply headers
        finalHeaders.forEach((key, value) -> request.addHeader(key, value));

        logger.debug("Request details - Method: {}, URL: {}", method, url);
        logger.debug("Request headers: {}", finalHeaders);
        if (body != null && !body.isEmpty()) {
            logger.debug("Request body: {}", body);
        }

        CompletableFuture<SimpleHttpResponse> responseFuture = new CompletableFuture<>();

        client.execute(
                request,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.info("Request completed with status: {}", result.getCode());
                        logger.debug("Response body: {}", result.getBodyText());
                        responseFuture.complete(result);
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("Request failed: {}", ex.getMessage());
                        responseFuture.completeExceptionally(ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.warn("Request was cancelled");
                        responseFuture.cancel(true);
                    }
                });
        return responseFuture;
    }

    public CompletableFuture<SimpleHttpResponse> get(String endpoint, Map<String, String> headers) {
        return sendRequest("GET", endpoint, null, headers);
    }

    public CompletableFuture<SimpleHttpResponse> post(String endpoint, String body, Map<String, String> headers) {
        return sendRequest("POST", endpoint, body, headers);
    }

    public CompletableFuture<SimpleHttpResponse> put(String endpoint, String body, Map<String, String> headers) {
        return sendRequest("PUT", endpoint, body, headers);
    }

    public CompletableFuture<SimpleHttpResponse> delete(String endpoint, Map<String, String> headers) {
        return sendRequest("DELETE", endpoint, null, headers);
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