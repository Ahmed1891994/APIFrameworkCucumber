package api.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class OutputFileHelper {
    private static final Logger logger = LoggerFactory.getLogger(OutputFileHelper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestDataStore requestDataStore;

    public OutputFileHelper(RequestDataStore requestDataStore) {
        this.requestDataStore = requestDataStore;
    }

    public void saveResponseToFile(String filename, boolean append) throws IOException {
        String response = (String) requestDataStore.getContextValue("response");
        if (response == null) {
            logger.error("❌ No response available to save to file: {}", filename);
            throw new IOException("No response available to save");
        }

        logger.info("💾 Saving response to file: {}", filename);
        logger.info("📄 Append mode: {}", append);
        logger.debug("Response content: {}", response);

        try {
            JsonNode newJsonNode = objectMapper.readTree(response);
            Path path = Paths.get(filename);

            if (append && Files.exists(path)) {
                logger.info("📝 Appending to existing file: {}", filename);
                appendToJsonArray(path, newJsonNode);
            } else {
                if (append) {
                    logger.info("🆕 File doesn't exist, creating new file: {}", filename);
                } else {
                    logger.info("🆕 Creating new file: {}", filename);
                }
                createNewJsonArray(path, newJsonNode);
            }

            logger.info("✅ Successfully saved response to file: {}", filename);

        } catch (Exception e) {
            logger.error("❌ Failed to save response to file: {}", filename, e);
            throw new IOException("Failed to save response to file: " + filename, e);
        }
    }

    private void appendToJsonArray(Path path, JsonNode newJsonNode) throws IOException {
        logger.debug("Reading existing content from file: {}", path);
        String existingContent = new String(Files.readAllBytes(path));

        logger.debug("Parsing existing JSON content");
        JsonNode existingNode = objectMapper.readTree(existingContent);

        ArrayNode arrayNode;
        if (existingNode.isArray()) {
            logger.debug("Existing file contains JSON array, adding new element");
            arrayNode = (ArrayNode) existingNode;
        } else {
            logger.debug("Existing file contains single JSON object, converting to array");
            arrayNode = objectMapper.createArrayNode();
            arrayNode.add(existingNode);
        }

        logger.debug("Adding new response to array");
        arrayNode.add(newJsonNode);

        logger.debug("Formatting JSON array with pretty printing");
        String beautifiedArray = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode);

        logger.debug("Writing updated content to file: {}", path);
        Files.write(path, beautifiedArray.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        logger.info("📊 File updated. Total elements in array: {}", arrayNode.size());
    }

    private void createNewJsonArray(Path path, JsonNode newJsonNode) throws IOException {
        logger.debug("Creating new JSON array with response data");
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(newJsonNode);

        logger.debug("Formatting JSON with pretty printing");
        String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode);

        logger.debug("Writing to new file: {}", path);
        Files.write(path, content.getBytes());

        logger.info("📁 New file created with 1 element");
    }
}