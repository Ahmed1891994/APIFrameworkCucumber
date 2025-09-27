package api.exceptions;

import com.networknt.schema.ValidationMessage;

import java.util.Set;

public class SchemaException extends ApiFrameworkException {

    public SchemaException(String message) {
        super(message, "SCHEMA_001", "Schema Validator");
    }

    public SchemaException(String message, Throwable cause) {
        super(message, "SCHEMA_001", "Schema Validator", cause);
    }

    public SchemaException(String message, String errorCode) {
        super(message, errorCode, "Schema Validator");
    }

    // Specific schema errors - simplified without unused fields
    public static SchemaException validationFailed(String schemaName, Set<ValidationMessage> errors) {
        StringBuilder message = new StringBuilder("Schema validation failed for " + schemaName + " with " + errors.size() + " errors:");
        for (ValidationMessage error : errors) {
            message.append("\n- ").append(error.getMessage());
        }

        return new SchemaException(message.toString(), "SCHEMA_002");
    }

    public static SchemaException schemaNotFound(String schemaPath) {
        return new SchemaException("Schema not found: " + schemaPath);
    }

    public static SchemaException invalidSchema(String schemaPath, Throwable cause) {
        return new SchemaException("Invalid schema format: " + schemaPath, cause);
    }
}