package api.definitions;

import api.helpers.TestContext;
import api.hooks.Hooks;
import io.cucumber.java.en.Then;

import java.io.IOException;

public class FileStepDefinitions {
    private final TestContext context;

    public FileStepDefinitions(Hooks hooks) {
        this.context = hooks.getContext();
    }

    @Then("save response to file {string}")
    public void saveResponseToFile(String filename) throws IOException {
        context.getFileHelper().saveResponseToFile(filename, false);
    }

    @Then("append response to file {string}")
    public void appendResponseToFile(String filename) throws IOException {
        context.getFileHelper().saveResponseToFile(filename, true);
    }
}