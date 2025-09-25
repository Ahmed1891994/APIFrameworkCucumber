package api.exceptions;

public class DataException extends ApiFrameworkException {
    public DataException(String message) {
        super(message);
    }

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}