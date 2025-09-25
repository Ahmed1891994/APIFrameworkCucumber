package api.exceptions;

public class RequestException extends ApiFrameworkException {
    private final int statusCode;

    public RequestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public RequestException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}