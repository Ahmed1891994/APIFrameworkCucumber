package api.exceptions;

public class HttpClientException extends ApiFrameworkException {
    public HttpClientException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, "HTTP Client", cause);
    }

    // Specific HTTP errors - simplified without unused parameters
    public static HttpClientException connectionError(String url, Throwable cause) {
        return new HttpClientException(
                "Connection failed to " + url,
                "HTTP_001", cause
        );
    }

    public static HttpClientException retryExhausted(String url, int maxRetries, Throwable lastError) {
        return new HttpClientException(
                "Request failed after " + maxRetries + " retries: " + url,
                "HTTP_004", lastError
        );
    }
}