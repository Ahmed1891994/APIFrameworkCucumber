package api.hooks;

import api.helpers.TestContext;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hooks {
    private TestContext context;
    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);

    public Hooks() {
        // Context will be initialized in setUp with the proper environment
    }

    @Before
    public void setUp(Scenario scenario) {
        String environment = System.getProperty("env", "dev");
        this.context = new TestContext(environment);

        String scenarioName = scenario.getName();
        logger.info("Starting scenario: {} in {} environment", scenarioName, environment);
        context.reset();
        context.setData("scenario_name", scenarioName);
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
            String request = context.getPathExtractor().getValue("last_request") != null ?
                    context.getPathExtractor().getValue("last_request").toString() : "No request captured";
            String response = context.getPathExtractor().getValue("last_response") != null ?
                    context.getPathExtractor().getValue("last_response").toString() : "No response captured";

            Allure.addAttachment("Request", "text/plain", request);
            Allure.addAttachment("Response", "text/plain", response);
        } catch (Exception e) {
            logger.warn("Failed to attach request/response to Allure", e);
        }
    }

    private void attachScenarioDataToAllure() {
        try {
            StringBuilder data = new StringBuilder();
            data.append("Extracted Values:\n");
            context.getPathExtractor().getAllValues().forEach((key, value) ->
                    data.append(key).append(": ").append(value).append("\n"));

            data.append("\nScenario Data:\n");
            context.getPathExtractor().getAllValues().forEach((key, value) ->
                    data.append(key).append(": ").append(value).append("\n"));

            Allure.addAttachment("Scenario Data", "text/plain", data.toString());
        } catch (Exception e) {
            logger.warn("Failed to attach scenario data to Allure", e);
        }
    }
}