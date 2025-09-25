package api.reporting;

import api.client.AsyncRestClient;
import io.qameta.allure.Allure;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class EnhancedAllureReporter {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAllureReporter.class);

    private static final DefaultAttachmentProcessor attachmentProcessor = new DefaultAttachmentProcessor();

    public static void attachRequest(SimpleHttpRequest request, AsyncRestClient client) {
        try {
            String requestBody = client.requestToString(request);
            String method = request.getMethod();
            String url = request.getRequestUri();

            // Create request attachment with Allure's standard format
            Map<String, String> headers = Arrays.stream(request.getHeaders())
                    .collect(Collectors.toMap(Header::getName, Header::getValue));

            HttpRequestAttachment requestAttachment = HttpRequestAttachment.Builder
                    .create("HTTP Request", url)
                    .setMethod(method)
                    .setHeaders(headers)
                    .setBody(requestBody)
                    .build();

            attachmentProcessor.addAttachment(
                    requestAttachment,
                    new FreemarkerAttachmentRenderer("http-request.ftl")
            );

        } catch (Exception e) {
            logger.warn("Failed to attach request to Allure", e);
        }
    }

    public static void attachResponse(SimpleHttpResponse response, AsyncRestClient client) {
        try {
            String responseBody = client.responseToString(response);
            int statusCode = response.getCode();

            Map<String, String> headers = Arrays.stream(response.getHeaders())
                    .collect(Collectors.toMap(Header::getName, Header::getValue));

            HttpResponseAttachment responseAttachment = HttpResponseAttachment.Builder
                    .create("HTTP Response")
                    .setResponseCode(statusCode)
                    .setHeaders(headers)
                    .setBody(responseBody)
                    .build();

            attachmentProcessor.addAttachment(
                    responseAttachment,
                    new FreemarkerAttachmentRenderer("http-response.ftl")
            );

        } catch (Exception e) {
            logger.warn("Failed to attach response to Allure", e);
        }
    }

    // Additional utility methods
    public static void attachText(String name, String content) {
        try {
            Allure.addAttachment(name, "text/plain", content);
        } catch (Exception e) {
            logger.warn("Failed to attach text to Allure", e);
        }
    }

    public static void attachCurlCommand(SimpleHttpRequest request, AsyncRestClient client) {
        try {
            String curlCommand = generateCurlCommand(request, client);
            Allure.addAttachment("cURL Command", "text/plain", curlCommand);
        } catch (Exception e) {
            logger.warn("Failed to attach cURL command to Allure", e);
        }
    }

    private static String generateCurlCommand(SimpleHttpRequest request, AsyncRestClient client) throws URISyntaxException {
        StringBuilder curl = new StringBuilder("curl -X ").append(request.getMethod());

        // Add headers
        for (Header header : request.getHeaders()) {
            curl.append(" \\\n  -H '").append(header.getName()).append(": ").append(header.getValue()).append("'");
        }

        // Add body if present
        String body = client.requestToString(request);
        if (body != null && !body.trim().isEmpty()) {
            curl.append(" \\\n  -d '").append(body.replace("'", "\\'")).append("'");
        }

        curl.append(" \\\n  '").append(request.getRequestUri()).append("'");

        return curl.toString();
    }
}