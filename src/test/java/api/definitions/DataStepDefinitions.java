package api.definitions;

import api.helpers.TestContext;
import api.hooks.Hooks;
import io.cucumber.java.en.Given;

import java.util.List;
import java.util.Map;

public class DataStepDefinitions {
    private final TestContext context;

    public DataStepDefinitions(Hooks hooks) {
        this.context = hooks.getContext();
    }

    @Given("generate random values")
    public void generateRandomValues(Map<String, String> patterns) {
        patterns.forEach((key, pattern) -> {
            String value = context.getRegexGenerator().generateValue(pattern);
            context.getRequestData().storeContextValue(key, value);
        });
    }

    @Given("load test data from {string}")
    public void loadTestData(String dataSource) {
        List<Map<String, String>> testData = context.getDataDrivenTestGenerator().loadTestData(dataSource);
        context.getRequestData().storeContextValue("test_data", testData);
    }

    @Given("use test data row {int}")
    public void useTestDataRow(int rowIndex) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> testData = (List<Map<String, String>>) context.getRequestData().getContextValue("test_data");
        if (testData == null || rowIndex >= testData.size()) {
            throw new RuntimeException("Test data not available or invalid row index: " + rowIndex);
        }
        Map<String, String> rowData = testData.get(rowIndex);
        rowData.forEach((key, value) ->
                context.getRequestData().storeContextValue(key, value));
    }

    @Given("add to path parameters from context")
    public void addPathParametersFromContext(Map<String, String> pathParams) {
        // This should store path params for later use, not modify endpoint immediately
        context.getRequestData().storeContextValue("path_params", pathParams);
    }

    // For query parameters
    @Given("add to query parameters from context")
    public void addQueryParametersFromContext(Map<String, String> queryParams) {
        context.getRequestData().storeContextValue("query_params", queryParams);
    }
}