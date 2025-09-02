package api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class SchemaValidator {
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public SchemaValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    public void validateSchema(String json, String schemaPath) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(json);
        JsonNode schemaNode = objectMapper.readTree(new File(schemaPath));
        JsonSchema schema = schemaFactory.getSchema(schemaNode);

        Set<ValidationMessage> errors = schema.validate(jsonNode);
        if (!errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Schema validation failed:\n");
            for (ValidationMessage error : errors) {
                errorMessage.append(error.getMessage()).append("\n");
            }
            throw new AssertionError(errorMessage.toString());
        }
    }
}