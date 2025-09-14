package api.helpers;

import api.exceptions.ResponseNotAvailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    public void saveResponseToFile(String filename, boolean append) {
        String response = (String) pathExtractor.getValue("response");
        if (response == null) {
            throw new ResponseNotAvailableException("No response available to save");
        }

        JsonNode newJsonNode;
        try {
            newJsonNode = objectMapper.readTree(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Path path = Paths.get(filename);

        if (append && Files.exists(path)) {
            try {
                appendToJsonArray(path, newJsonNode);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            createNewJsonArray(path, newJsonNode);
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

    private void createNewJsonArray(Path path, JsonNode newJsonNode) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(newJsonNode);
        String content;
        try {
            content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.write(path, content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}