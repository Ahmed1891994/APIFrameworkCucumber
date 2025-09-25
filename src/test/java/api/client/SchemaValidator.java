package api.client;

import api.exceptions.SchemaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SchemaValidator {
    private static final Logger logger = LoggerFactory.getLogger(SchemaValidator.class);
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final Map<String, JsonSchema> schemaCache;

    public SchemaValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.schemaCache = new ConcurrentHashMap<>();
        logger.info("SchemaValidator initialized");
    }

    public void validateSchema(String json, String schemaName) throws IOException {
        validateSchemaInternal(json, schemaName, "schemas/" + schemaName + ".json");
    }

    public void validateSchemaAgainstClasspath(String json, String classpathSchema) throws IOException {
        String schemaPath = classpathSchema.endsWith(".json") ? classpathSchema : classpathSchema + ".json";
        if (!schemaPath.startsWith("schemas/")) {
            schemaPath = "schemas/" + schemaPath;
        }
        validateSchemaInternal(json, classpathSchema, schemaPath);
    }

    private void validateSchemaInternal(String json, String schemaName, String schemaPath) throws IOException {
        logger.info("Validating response against schema: {}", schemaName);
        logger.debug("Response body to validate: {}", json);

        JsonNode jsonNode = objectMapper.readTree(json);
        JsonSchema schema = loadSchemaFromClasspath(schemaPath);

        Set<ValidationMessage> errors = schema.validate(jsonNode);
        if (!errors.isEmpty()) {
            logger.warn("Schema validation failed for {} with {} errors", schemaName, errors.size());

            StringBuilder errorMessage = new StringBuilder("Schema validation failed:\n");
            for (ValidationMessage error : errors) {
                errorMessage.append(error.getMessage()).append("\n");
                logger.warn("Validation error: {}", error.getMessage());
            }
            throw new SchemaException(errorMessage.toString());
        }

        logger.info("Schema validation successful for: {}", schemaName);
    }

    private JsonSchema loadSchemaFromClasspath(String classpathSchema) {
        return schemaCache.computeIfAbsent(classpathSchema, path -> {
            logger.debug("Loading schema from classpath: {}", path);

            try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(path)) {
                if (schemaStream == null) {
                    String errorMsg = "Schema not found in classpath: " + path;
                    logger.error(errorMsg);
                    throw new SchemaException(errorMsg);
                }

                JsonNode schemaNode = objectMapper.readTree(schemaStream);
                JsonSchema schema = schemaFactory.getSchema(schemaNode);
                logger.debug("Successfully loaded and parsed schema: {}", path);
                return schema;
            } catch (IOException e) {
                String errorMsg = "Failed to load schema from classpath: " + path;
                logger.error(errorMsg, e);
                throw new SchemaException(errorMsg, e);
            }
        });
    }

    public void clearCache() {
        int cacheSize = schemaCache.size();
        schemaCache.clear();
        logger.debug("Cleared schema cache ({} schemas removed)", cacheSize);
    }
}