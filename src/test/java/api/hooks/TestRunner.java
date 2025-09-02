package api.hooks;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"api.definitions", "api.hooks"},
        plugin = {
                "pretty",
                "json:target/cucumber-reports/cucumber.json",
                "html:target/cucumber-reports/cucumber.html",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true
)
public class TestRunner extends AbstractTestNGCucumberTests {
        private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

        @BeforeSuite
        public void beforeSuite() {
                logger.info("Starting API test suite");
        }

        @AfterSuite
        public void afterSuite() {
                logger.info("Completed API test suite");
        }
}