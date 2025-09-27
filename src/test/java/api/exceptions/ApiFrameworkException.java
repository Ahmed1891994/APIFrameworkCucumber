package api.exceptions;

public abstract class ApiFrameworkException extends RuntimeException {
    private final String errorCode;
    private final long timestamp;
    private final String component;

    public ApiFrameworkException(String message, String errorCode, String component) {
        super(message);
        this.errorCode = errorCode;
        this.component = component;
        this.timestamp = System.currentTimeMillis();
    }

    public ApiFrameworkException(String message, String errorCode, String component, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.component = component;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getErrorCode() { return errorCode; }
    public long getTimestamp() { return timestamp; }
    public String getComponent() { return component; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (Component: %s, Time: %d)",
                errorCode, getClass().getSimpleName(), getMessage(), component, timestamp);
    }
}