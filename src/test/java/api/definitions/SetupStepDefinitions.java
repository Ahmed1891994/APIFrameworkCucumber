package api.definitions;

import api.helpers.TestContext;
import api.hooks.Hooks;
import api.client.AsyncRestClient;
import io.cucumber.java.en.Given;

public class SetupStepDefinitions {
    private final TestContext context;
    private final AsyncRestClient client;

    public SetupStepDefinitions(Hooks hooks) {
        this.context = hooks.getContext();
        this.client = context.getRestClient();
    }

    @Given("set base url to {string}")
    public void setBaseUrl(String baseUrl) {
        client.setBaseUrl(baseUrl);
    }

    @Given("set endpoint to {string}")
    public void setEndpoint(String path) {
        context.getRequestData().setEndpoint(path);
    }

    @Given("apply authentication {string}")
    public void applyAuthentication(String authType) {
        context.getRequestData().setAuthType(authType);
    }
}