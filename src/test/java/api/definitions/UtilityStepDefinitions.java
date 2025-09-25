package api.definitions;

import api.helpers.TestContext;
import api.hooks.Hooks;
import api.performance.PerformanceMonitor;
import io.cucumber.java.en.Then;

public class UtilityStepDefinitions {
    private final TestContext context;

    public UtilityStepDefinitions(Hooks hooks) {
        this.context = hooks.getContext();
    }

    @Then("wait for {int} seconds")
    public void waitForSeconds(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
    }

    @Then("verify last request response time is less than {long} ms")
    public void verifyLastRequestResponseTime(long maxAllowedMs) {
        PerformanceMonitor.PerformanceReport report = context.getPerformanceMonitor().generateReport();
        String lastRequestKey = context.getPerformanceMonitor().getLastRequestKey();
        if (lastRequestKey != null) {
            report.assertLastRequestMaxResponseTime(lastRequestKey, maxAllowedMs);
        } else {
            throw new AssertionError("No performance data available for last request");
        }
    }
}