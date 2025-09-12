package api.reporting;

import api.client.AsyncRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class EnhancedAllureReporter {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAllureReporter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void attachRequest(SimpleHttpRequest request, AsyncRestClient client) {
        try {
            Allure.addAttachment("Request", "text/plain", client.requestToString(request));
            Allure.addAttachment("Request Headers", "application/json",
                    objectMapper.writeValueAsString(
                            Arrays.stream(request.getHeaders())
                                    .collect(Collectors.toMap(
                                            Header::getName,
                                            Header::getValue
                                    ))
                    ));
        } catch (Exception e) {
            logger.warn("Failed to attach request to Allure", e);
        }
    }

    public static void attachResponse(SimpleHttpResponse response, AsyncRestClient client) {
        try {
            Allure.addAttachment("Response", "text/plain", client.responseToString(response));
            Allure.addAttachment("Response Headers", "application/json",
                    objectMapper.writeValueAsString(
                            Arrays.stream(response.getHeaders())
                                    .collect(Collectors.toMap(
                                            Header::getName,
                                            Header::getValue
                                    ))
                    ));
            Allure.addAttachment("Status Code", "text/plain", String.valueOf(response.getCode()));
        } catch (Exception e) {
            logger.warn("Failed to attach response to Allure", e);
        }
    }

    public static void attachJson(String name, String json) {
        try {
            Allure.addAttachment(name, "application/json", json);
        } catch (Exception e) {
            logger.warn("Failed to attach JSON to Allure", e);
        }
    }
}