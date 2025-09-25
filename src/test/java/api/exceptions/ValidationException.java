package api.exceptions;

public class ValidationException extends ApiFrameworkException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}