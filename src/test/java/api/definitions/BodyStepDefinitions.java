package api.definitions;

import api.helpers.JsonCreatorHelper;
import api.helpers.TestContext;
import api.hooks.Hooks;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cucumber.java.en.Given;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BodyStepDefinitions {
    private final TestContext context;
    public BodyStepDefinitions(Hooks hooks) {
        this.context = hooks.getContext();
    }

    @Given("set JSON body {string} with:")
    public void setJsonBody(String bodyName, Map<String, String> bodyProperties) {
        ObjectNode jsonBody = context.getRequestData().getOrCreateJsonBodyNode(bodyName);
        JsonCreatorHelper.addCucumberValues(jsonBody, bodyProperties);
        context.getRequestData().setJsonBody(bodyName, jsonBody);
    }

    @Given("add to JSON body {string} from context")
    public void addToJsonBodyFromContext(String bodyName, Map<String, String> contextMappings) {
        final ObjectNode finalJsonBody = context.getRequestData().getOrCreateJsonBodyNode(bodyName);

        contextMappings.forEach((jsonKey, contextKey) -> {
            Object value = context.getRequestData().getContextValue(contextKey);
            if (value != null) {
                if (value instanceof String) {
                    value = JsonCreatorHelper.convertCucumberValue((String) value);
                }
                JsonCreatorHelper.addValue(finalJsonBody, jsonKey, value);
            }
        });

        context.getRequestData().setJsonBody(bodyName, finalJsonBody);
    }

    @Given("remove from JSON body {string} keys")
    public void removeFromJsonBody(String bodyName, List<String> keys) {
        context.getRequestData().removeKeysFromBody(bodyName, keys);
    }

    @Given("remove from JSON body {string} key {string}")
    public void removeFromJsonBody(String bodyName, String key) {
        context.getRequestData().removeKeysFromBody(bodyName, Collections.singletonList(key));
    }

    @Given("set empty JSON body {string}")
    public void setEmptyJsonBody(String bodyName) {
        context.getRequestData().setEmptyJsonBody(bodyName);
    }

    @Given("clear body {string}")
    public void clearBody(String bodyName) {
        context.getRequestData().clearBody(bodyName);
    }
}