package api.hooks;

import api.helpers.TestContext;
import api.reporting.EnhancedAllureReporter;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hooks {
    private TestContext context;
    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);

    public Hooks() {

    }

    @Before
    public void setUp(Scenario scenario) {
        String environment = System.getProperty("env", "dev");
        this.context = new TestContext(environment);

        String scenarioName = scenario.getName();
        logger.info("Starting scenario: {} in {} environment", scenarioName, environment);
        context.reset();
        context.getRequestData().storeContextValue("scenario_name", scenarioName);
    }

    @After
    public void tearDown(Scenario scenario) {
        String scenarioName = scenario.getName();
        if (scenario.isFailed()) {
            logger.error("Scenario failed: {}", scenarioName);
            attachRequestResponseToAllure();
            attachScenarioDataToAllure();
        } else {
            logger.info("Scenario passed: {}", scenarioName);
        }

        // Generate performance report for the scenario
        context.getPerformanceMonitor().generateReport().printReport();

        context.getRestClient().close();
    }

    public TestContext getContext() {
        return context;
    }

    private void attachRequestResponseToAllure() {
        try {
            // Get the last request and response from your context
            Object lastRequest = context.getRequestData().getContextValue("last_request");
            Object lastResponse = context.getRequestData().getContextValue("last_response");

            // Check if they are the proper types for your EnhancedAllureReporter
            if (lastRequest instanceof org.apache.hc.client5.http.async.methods.SimpleHttpRequest &&
                    lastResponse instanceof org.apache.hc.client5.http.async.methods.SimpleHttpResponse) {

                EnhancedAllureReporter.attachRequest(
                        (org.apache.hc.client5.http.async.methods.SimpleHttpRequest) lastRequest,
                        context.getRestClient()
                );

                EnhancedAllureReporter.attachResponse(
                        (org.apache.hc.client5.http.async.methods.SimpleHttpResponse) lastResponse,
                        context.getRestClient()
                );

                EnhancedAllureReporter.attachCurlCommand(
                        (org.apache.hc.client5.http.async.methods.SimpleHttpRequest) lastRequest,
                        context.getRestClient()
                );
            } else {
                // Fallback to basic attachment if types don't match
                String request = lastRequest != null ? lastRequest.toString() : "No request captured";
                String response = lastResponse != null ? lastResponse.toString() : "No response captured";

                EnhancedAllureReporter.attachText("Request", request);
                EnhancedAllureReporter.attachText("Response", response);
            }
        } catch (Exception e) {
            logger.warn("Failed to attach request/response to Allure", e);
        }
    }

    private void attachScenarioDataToAllure() {
        try {
            StringBuilder data = new StringBuilder();
            data.append("=== SCENARIO DATA ===\n\n");

            data.append("Extracted Values:\n");
            context.getRequestData().getAllContextValues().forEach((key, value) ->
                    data.append("  ").append(key).append(": ").append(value).append("\n"));

            data.append("\nPerformance Metrics:\n");
            // Add performance metrics if available
            data.append(context.getPerformanceMonitor().generateReport().toString());

            EnhancedAllureReporter.attachText("Scenario Data", data.toString());

        } catch (Exception e) {
            logger.warn("Failed to attach scenario data to Allure", e);
        }
    }
}