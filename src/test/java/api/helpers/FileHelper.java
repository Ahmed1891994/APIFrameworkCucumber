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

public class FileHelper {
    private static final Logger logger = LoggerFactory.getLogger(FileHelper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PathExtractor pathExtractor;

    public FileHelper(PathExtractor pathExtractor) {
        this.pathExtractor = pathExtractor;
    }

    public void saveResponseToFile(String filename, boolean append) throws IOException {
        String response = (String) pathExtractor.getValue("response");
        if (response == null) {
            throw new IOException("No response available to save");
        }

        try {
            JsonNode newJsonNode = objectMapper.readTree(response);
            Path path = Paths.get(filename);

            if (append && Files.exists(path)) {
                appendToJsonArray(path, newJsonNode);
            } else {
                createNewJsonArray(path, newJsonNode);
            }

        } catch (Exception e) {
            throw new IOException("Failed to save response to file: " + filename, e);
        }
    }

    private void appendToJsonArray(Path path, JsonNode newJsonNode) throws IOException {
        String existingContent = new String(Files.readAllBytes(path));
        JsonNode existingNode = objectMapper.readTree(existingContent);

        ArrayNode arrayNode;
        if (existingNode.isArray()) {
            arrayNode = (ArrayNode) existingNode;
        } else {
            arrayNode = objectMapper.createArrayNode();
            arrayNode.add(existingNode);
        }

        arrayNode.add(newJsonNode);
        String beautifiedArray = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode);
        Files.write(path, beautifiedArray.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void createNewJsonArray(Path path, JsonNode newJsonNode) throws IOException {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(newJsonNode);
        String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode);
        Files.write(path, content.getBytes());
    }
}