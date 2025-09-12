package api.exceptions;

public class ApiFrameworkException extends RuntimeException {
    public ApiFrameworkException(String message) {
        super(message);
    }

    public ApiFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}