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
import java.util.*;

public class DataDrivenTestGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DataDrivenTestGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, String>> loadTestData(String source) {
        logger.info("Loading test data from: {}", source);

        // Remove any prefix and detect format by extension
        String actualPath = source.replaceFirst("^(classpath:|file:|csv:|json:)", "");

        if (source.endsWith(".csv") || source.startsWith("csv:")) {
            return loadFromCsv(actualPath);
        } else if (source.endsWith(".json") || source.startsWith("json:")) {
            return loadFromJson(actualPath);
        } else {
            try {
                return loadFromClasspath(actualPath);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unsupported data source: " + source +
                        ". Supported formats: .csv, .json");
            }
        }
    }

    private List<Map<String, String>> loadFromClasspath(String path) {
        logger.info("Loading from classpath: {}", path);

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
        logger.info("Loading CSV from: {}", path);

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
        logger.info("Loading JSON from: {}", path);

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
                     .withSkipLines(0)
                     .build()) {

            // Read all data
            List<String[]> allData = csvReader.readAll();

            if (logger.isDebugEnabled()) {
                logger.debug("Raw CSV data: {}", Arrays.deepToString(allData.toArray()));
            }

            if (allData.isEmpty()) {
                logger.warn("CSV file is empty");
                return result;
            }

            // First row is headers
            String[] headers = allData.getFirst();
            logger.info("Detected headers: {}", Arrays.toString(headers));

            // Process data rows
            for (int i = 1; i < allData.size(); i++) {
                String[] row = allData.get(i);

                if (logger.isTraceEnabled()) {
                    logger.trace("Row {} raw: {}", i, Arrays.toString(row));
                }

                // Check if the row has the correct number of columns
                if (row.length < headers.length) {
                    logger.warn("Row {} has {} columns, expected {} → padding with empty values.",
                            i, row.length, headers.length);

                    String[] newRow = new String[headers.length];
                    System.arraycopy(row, 0, newRow, 0, row.length);
                    Arrays.fill(newRow, row.length, headers.length, "");
                    row = newRow;
                }

                Map<String, String> rowMap = new HashMap<>();
                for (int j = 0; j < headers.length; j++) {
                    String value = row[j] != null ? row[j].trim() : "";
                    rowMap.put(headers[j], value);
                }

                result.add(rowMap);
                logger.debug("Row {} parsed: {}", i, rowMap);
            }
        }

        logger.info("Parsed {} rows from CSV successfully", result.size());
        return result;
    }

    private List<Map<String, String>> loadJsonFromStream(InputStream is) throws IOException {
        logger.info("Loading JSON from stream");

        List<Map<String, String>> result = objectMapper.readValue(is, new TypeReference<>() {});

        logger.info("Parsed {} rows from JSON", result.size());

        if (logger.isDebugEnabled()) {
            logger.debug("Parsed JSON content: {}", result);
        }

        return result;
    }
}