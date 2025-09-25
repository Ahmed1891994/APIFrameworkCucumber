package api.client;

import api.config.Configuration;
import api.performance.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AsyncRestClient {
    private static final Logger logger = LoggerFactory.getLogger(AsyncRestClient.class);
    private final CloseableHttpAsyncClient client;
    private final PerformanceMonitor performanceMonitor;

    private String baseUrl;

    public AsyncRestClient(PerformanceMonitor performanceMonitor, Configuration configuration) {
        this(createConfiguredHttpClient(configuration), performanceMonitor, configuration);
    }

    public AsyncRestClient(CloseableHttpAsyncClient customClient, PerformanceMonitor performanceMonitor, Configuration configuration) {
        logger.info("Initializing AsyncRestClient");
        this.client = customClient;
        this.performanceMonitor = performanceMonitor;
        this.baseUrl = configuration.getBaseUrl();
        this.client.start();
        logger.info("AsyncRestClient initialized with base URL: {} and timeouts: request={}s, response={}s",
                baseUrl, configuration.getRequestTimeout(), configuration.getResponseTimeout());
    }

    // Helper method to create HTTP client with configuration
    private static CloseableHttpAsyncClient createConfiguredHttpClient(Configuration configuration) {
        int requestTimeout = configuration.getRequestTimeout();
        int responseTimeout = configuration.getResponseTimeout();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(requestTimeout))
                .setResponseTimeout(Timeout.ofSeconds(responseTimeout))
                .build();

        return HttpAsyncClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public void setBaseUrl(String baseUrl) {
        logger.info("Setting base URL to: {}", baseUrl);
        this.baseUrl = baseUrl;
    }


    public CompletableFuture<SimpleHttpResponse> sendRequest(String method, String url, String body, Map<String, String> finalHeaders, int maxRetries) {
        SimpleHttpRequest request;
        SimpleRequestBuilder builder = SimpleRequestBuilder.create(method)
                .setUri(url);
        if (body != null && !body.isEmpty() && (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH"))) {

            builder.setBody(body, null);
        }
        finalHeaders.forEach(builder::addHeader);
        request = builder.build();

        // Debug: Log actual headers being sent
        logger.info("Actual request headers:");
        Arrays.stream(request.getHeaders()).forEach(h ->
                logger.info("  {}: {}", h.getName(), h.getValue()));


        CompletableFuture<SimpleHttpResponse> responseFuture = new CompletableFuture<>();

        logger.info("🚀 Sending {} request to: {}", method, url);
        logger.info("Request body: {}", body);
        logger.info("Request headers: {}", finalHeaders);

        // Execute with retry logic
        executeWithRetry(method, url, request, responseFuture, maxRetries, 0);

        return responseFuture;
    }

    private void executeWithRetry(String method, String endpoint, SimpleHttpRequest request,
                                  CompletableFuture<SimpleHttpResponse> responseFuture,
                                  int maxRetries, int attempt) {
        // Start measuring time at the HTTP level
        long startTime = System.currentTimeMillis();

        client.execute(
                request,
                new FutureCallback<>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        // Stop measuring time and calculate duration
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;

                        performanceMonitor.recordResponseTime(method, endpoint, duration);

                        logger.info("✅ Request completed with status: {}", result.getCode());
                        logger.info("Response body: {}", result.getBodyText());
                        logger.info("Request took {}ms", duration);

                        responseFuture.complete(result);
                    }

                    @Override
                    public void failed(Exception ex) {
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        logger.error("❌ Request failed after {}ms: {}", duration, ex.getMessage());

                        if (attempt < maxRetries) {
                            logger.info("Retrying request (attempt {}/{})", attempt + 1, maxRetries);
                            try {
                                Thread.sleep(1000 * (long) Math.pow(2, attempt)); // Exponential backoff
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            executeWithRetry(method, endpoint, request, responseFuture, maxRetries, attempt + 1);
                        } else {
                            responseFuture.completeExceptionally(ex);
                        }
                    }

                    @Override
                    public void cancelled() {
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        logger.warn("Request was cancelled after {}ms", duration);
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

    public void close() {
        logger.info("Closing AsyncRestClient");
        try {
            client.close();
        } catch (Exception e) {
            logger.error("Error closing AsyncRestClient", e);
        }
    }
}