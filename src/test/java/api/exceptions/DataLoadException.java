package api.exceptions;

public class DataLoadException extends ApiFrameworkException {

    public DataLoadException(String message) {
        super(message, "DATA_001", "Data Loader");
    }

    public DataLoadException(String message, Throwable cause) {
        super(message, "DATA_001", "Data Loader", cause);
    }

    // Specific data loading errors
    public static DataLoadException fileNotFound(String dataSource, String format) {
        return new DataLoadException("Data file " + format +" not found: " +  dataSource);
    }

    public static DataLoadException parseError(String dataSource, String format, Throwable cause) {
        return new DataLoadException("Failed to parse " + format + " data from: " + dataSource, cause);
    }

    public static DataLoadException unsupportedFormat(String dataSource, String format) {
        return new DataLoadException("Unsupported data format: " + format + " for source: " + dataSource);
    }
}