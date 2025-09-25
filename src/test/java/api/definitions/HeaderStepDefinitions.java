package api.definitions;

import api.helpers.TestContext;
import api.hooks.Hooks;
import io.cucumber.java.en.Given;

import java.util.List;
import java.util.Map;

public class HeaderStepDefinitions {
    private final TestContext context;

    public HeaderStepDefinitions(Hooks hooks) {
        this.context = hooks.getContext();
    }

    // ==================== REQUEST HEADERS MANAGEMENT STEPS ====================
    @Given("add to headers")
    public void addHeaders(Map<String, String> headerMap) {
        addHeaders(null, headerMap);
    }

    @Given("add to headers {string}")
    public void addHeaders(String headerSetName, Map<String, String> headerMap) {
        context.getRequestData().addHeaders(headerSetName, headerMap);
    }

    @Given("clear headers")
    public void clearHeaders() {
        clearHeaders(null);
    }

    @Given("clear headers {string}")
    public void clearHeaders(String headerSetName) {
        context.getRequestData().clearHeaders(headerSetName);
    }

    @Given("remove headers")
    public void removeHeaders(List<String> headerKeys) {
        removeHeaders(null, headerKeys);
    }

    @Given("remove headers {string}")
    public void removeHeaders(String headerSetName, List<String> headerKeys) {
        context.getRequestData().removeHeaders(headerSetName, headerKeys);
    }
}