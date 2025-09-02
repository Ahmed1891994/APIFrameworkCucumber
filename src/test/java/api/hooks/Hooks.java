package api.hooks;

import api.helpers.TestContext;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hooks {
    private final TestContext context;
    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);

    public Hooks() {
        // Default constructor required by Cucumber
        this.context = new TestContext();
    }

    @Before
    public void setUp(Scenario scenario) {
        // Initialize any required resources
        logger.info("Starting scenario: {}", scenario.getName());
        context.reset();
    }

    @After
    public void tearDown(Scenario scenario) {
        if (scenario.isFailed()) {
            logger.error("Scenario failed: {}", scenario.getName());
            attachRequestResponseToAllure();
        } else {
            logger.info("Scenario passed: {}", scenario.getName());
        }
        context.getRestClient().close();
    }

    public TestContext getContext() {
        return context;
    }

    private void attachRequestResponseToAllure() {
        try {
            String request = context.getPathExtractor().getValue("last_request").toString();
            String response = context.getPathExtractor().getValue("response").toString();

            Allure.addAttachment("Request", "application/json", request);
            Allure.addAttachment("Response", "application/json", response);
        } catch (Exception e) {
            logger.warn("Failed to attach request/response to Allure", e);
        }
    }
}