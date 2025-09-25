package api.definitions;

import api.helpers.TestContext;
import io.cucumber.java.en.Given;

public class ConfigurationStepDefinitions {
    private final TestContext context;

    public ConfigurationStepDefinitions(TestContext context) {
        this.context = context;
    }

    @Given("for the next request, disable URL encoding")
    public void disableUrlEncodingForNextRequest() {
        context.getRequestData().setUrlEncodingForNextRequest(false);
    }

    @Given("for the next request, enable URL encoding")
    public void enableUrlEncodingForNextRequest() {
        context.getRequestData().setUrlEncodingForNextRequest(true);
    }
}