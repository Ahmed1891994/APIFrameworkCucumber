package api.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DataDrivenTestGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DataDrivenTestGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static boolean debugMode = false;

    // Method to set debug mode from configuration
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
        if (debugMode) {
            logger.info("Debug mode enabled for DataDrivenTestGenerator");
        }
    }

    public List<Map<String, String>> loadTestData(String source) {
        if (debugMode) {
            logger.info("Loading test data from: {}", source);
        }

        // Remove any prefix and detect format by extension
        String actualPath = source.replaceFirst("^(classpath:|file:|csv:|json:)", "");

        if (source.endsWith(".csv") || source.startsWith("csv:")) {
            return loadFromCsv(actualPath);
        } else if (source.endsWith(".json") || source.startsWith("json:")) {
            return loadFromJson(actualPath);
        } else {
            // Try to auto-detect from classpath
            try {
                return loadFromClasspath(actualPath);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unsupported data source: " + source +
                        ". Supported formats: .csv, .json");
            }
        }
    }

    private List<Map<String, String>> loadFromClasspath(String path) {
        if (debugMode) {
            logger.info("Loading from classpath: {}", path);
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }

            if (path.endsWith(".csv")) {
                return loadCsvFromStream(is);
            } else if (path.endsWith(".json")) {
                return loadJsonFromStream(is);
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + path);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load test data from classpath: " + path, e);
        }
    }

    private List<Map<String, String>> loadFromCsv(String path) {
        if (debugMode) {
            logger.info("Loading CSV from: {}", path);
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("CSV file not found: " + path);
            }
            return loadCsvFromStream(is);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load CSV data: " + path, e);
        }
    }

    private List<Map<String, String>> loadFromJson(String path) {
        if (debugMode) {
            logger.info("Loading JSON from: {}", path);
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("JSON file not found: " + path);
            }
            return loadJsonFromStream(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON data: " + path, e);
        }
    }

    private List<Map<String, String>> loadCsvFromStream(InputStream is) throws IOException, CsvException {
        List<Map<String, String>> result = new ArrayList<>();

        try (Reader reader = new InputStreamReader(is);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withSkipLines(0) // Don't skip any lines
                     .build()) {

            // Read all data
            List<String[]> allData = csvReader.readAll();

            if (debugMode) {
                logger.info("Raw CSV data: {}", Arrays.deepToString(allData.toArray()));
            }

            if (allData.isEmpty()) {
                if (debugMode) {
                    logger.info("CSV file is empty");
                }
                return result;
            }

            // First row is headers
            String[] headers = allData.get(0);
            if (debugMode) {
                logger.info("Headers: {}", Arrays.toString(headers));
            }

            // Process data rows
            for (int i = 1; i < allData.size(); i++) {
                String[] row = allData.get(i);

                if (debugMode) {
                    logger.info("Row {}: {}", i, Arrays.toString(row));
                }

                // Check if the row has the correct number of columns
                if (row.length < headers.length) {
                    logger.warn("Row {} has {} columns, expected {}. Padding with empty values.",
                            i, row.length, headers.length);
                    // Pad the row with empty values
                    row = Arrays.copyOf(row, headers.length);
                    for (int j = row.length; j < headers.length; j++) {
                        row[j] = "";
                    }
                }

                Map<String, String> rowMap = new HashMap<>();
                for (int j = 0; j < headers.length && j < row.length; j++) {
                    String value = row[j] != null ? row[j].trim() : "";
                    rowMap.put(headers[j], value);
                }

                result.add(rowMap);

                if (debugMode) {
                    logger.info("Row {} parsed: {}", i, rowMap);
                }
            }
        }

        logger.info("Successfully parsed {} rows from CSV", result.size());

        if (debugMode && !result.isEmpty()) {
            logger.info("Final parsed data: {}", result);
        }

        return result;
    }

    private List<Map<String, String>> loadJsonFromStream(InputStream is) throws IOException {
        if (debugMode) {
            logger.info("Loading JSON from stream");
        }

        List<Map<String, String>> result = objectMapper.readValue(is, new TypeReference<List<Map<String, String>>>() {});

        if (debugMode) {
            logger.info("Successfully parsed {} rows from JSON: {}", result.size(), result);
        }

        return result;
    }

    private List<Map<String, String>> loadFromFile(String path) {
        if (debugMode) {
            logger.info("Loading from file: {}", path);
        }

        try {
            if (path.endsWith(".csv")) {
                try (Reader reader = Files.newBufferedReader(Paths.get(path));
                     CSVReader csvReader = new CSVReaderBuilder(reader).build()) {

                    List<String[]> allData = csvReader.readAll();
                    List<Map<String, String>> result = new ArrayList<>();

                    if (allData.isEmpty()) {
                        return result;
                    }

                    String[] headers = allData.get(0);

                    for (int i = 1; i < allData.size(); i++) {
                        String[] row = allData.get(i);
                        Map<String, String> rowMap = new HashMap<>();

                        // Pad row if necessary
                        if (row.length < headers.length) {
                            row = Arrays.copyOf(row, headers.length);
                            for (int j = row.length; j < headers.length; j++) {
                                row[j] = "";
                            }
                        }

                        for (int j = 0; j < headers.length && j < row.length; j++) {
                            rowMap.put(headers[j], row[j] != null ? row[j].trim() : "");
                        }

                        result.add(rowMap);
                    }

                    return result;
                }
            } else if (path.endsWith(".json")) {
                return objectMapper.readValue(Files.newInputStream(Paths.get(path)),
                        new TypeReference<List<Map<String, String>>>() {});
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + path);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load data from file: " + path, e);
        }
    }

    // Helper method to debug CSV parsing issues
    public static void debugCsvParsing(String csvContent) {
        try {
            List<String[]> data = new CSVReaderBuilder(new java.io.StringReader(csvContent)).build().readAll();
            System.out.println("=== CSV PARSING DEBUG ===");
            for (int i = 0; i < data.size(); i++) {
                System.out.println("Line " + i + ": " + Arrays.toString(data.get(i)));
            }
        } catch (Exception e) {
            System.out.println("CSV parsing failed: " + e.getMessage());
        }
    }
}