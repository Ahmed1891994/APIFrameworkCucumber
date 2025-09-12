package api.client;

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

    /**
     * Validate JSON against a schema located in src/test/resources/schemas/
     */
    public void validateSchema(String json, String schemaName) throws IOException {
        logger.info("Validating response against schema: {}", schemaName);

        if (logger.isDebugEnabled()) {
            logger.debug("Response body to validate: {}", json);
        }

        String schemaPath = "schemas/" + schemaName + ".json"; // always classpath
        JsonNode jsonNode = objectMapper.readTree(json);
        JsonSchema schema = loadSchemaFromClasspath(schemaPath);

        Set<ValidationMessage> errors = schema.validate(jsonNode);
        if (!errors.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Schema validation found {} errors", errors.size());
            }

            StringBuilder errorMessage = new StringBuilder("Schema validation failed:\n");
            for (ValidationMessage error : errors) {
                errorMessage.append(error.getMessage()).append("\n");
                if (logger.isDebugEnabled()) {
                    logger.debug("Validation error: {}", error.getMessage());
                }
            }
            throw new AssertionError(errorMessage.toString());
        }

        logger.info("✅ Schema validation successful for: {}", schemaName);
    }

    /**
     * Validate JSON against a schema explicitly from classpath
     */
    public void validateSchemaAgainstClasspath(String json, String classpathSchema) throws IOException {
        logger.info("Validating response against classpath schema: {}", classpathSchema);

        if (logger.isDebugEnabled()) {
            logger.debug("Response body to validate: {}", json);
        }

        if (!classpathSchema.endsWith(".json")) {
            classpathSchema = classpathSchema + ".json";
        }

        String schemaPath = "schemas/" + classpathSchema; // if full name passed
        JsonNode jsonNode = objectMapper.readTree(json);
        JsonSchema schema = loadSchemaFromClasspath(schemaPath);

        Set<ValidationMessage> errors = schema.validate(jsonNode);
        if (!errors.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Schema validation found {} errors", errors.size());
            }

            StringBuilder errorMessage = new StringBuilder("Schema validation failed:\n");
            for (ValidationMessage error : errors) {
                errorMessage.append(error.getMessage()).append("\n");
                if (logger.isDebugEnabled()) {
                    logger.debug("Validation error: {}", error.getMessage());
                }
            }
            throw new AssertionError(errorMessage.toString());
        }

        logger.info("✅ Schema validation successful for: {}", classpathSchema);
    }

    /**
     * Load schema from classpath and cache it
     */
    private JsonSchema loadSchemaFromClasspath(String classpathSchema) {
        return schemaCache.computeIfAbsent(classpathSchema, path -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Loading schema from classpath: {}", path);
            }

            try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(path)) {
                if (schemaStream == null) {
                    String errorMsg = "Schema not found in classpath: " + path;
                    logger.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }

                JsonNode schemaNode = objectMapper.readTree(schemaStream);
                JsonSchema schema = schemaFactory.getSchema(schemaNode);

                if (logger.isDebugEnabled()) {
                    logger.debug("Successfully loaded and parsed schema: {}", path);
                }

                return schema;
            } catch (IOException e) {
                String errorMsg = "Failed to load schema from classpath: " + path;
                logger.error(errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
        });
    }

    /**
     * Clear cached schemas
     */
    public void clearCache() {
        int cacheSize = schemaCache.size();
        schemaCache.clear();
        logger.info("Cleared schema cache ({} schemas removed)", cacheSize);
    }

    /**
     * Get number of cached schemas (for debugging)
     */
    public int getCacheSize() {
        return schemaCache.size();
    }

    /**
     * Check if schema is cached
     */
    public boolean isSchemaCached(String schemaName) {
        return schemaCache.containsKey("schemas/" + schemaName + ".json");
    }
}