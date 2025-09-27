package api.hooks;

import api.config.Configuration;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

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

        @Override
        @DataProvider(parallel = true)
        public Object[][] scenarios() {
                // This enables parallel scenario execution
                return super.scenarios();
        }

        @BeforeSuite
        public void beforeSuite() {
                logger.info("Starting API test suite");

                // Log parallel configuration
                Configuration config = new Configuration(System.getProperty("env", "dev"));
                logger.info("Parallel execution: {}", config.isParallelEnabled());
                if (config.isParallelEnabled()) {
                        logger.info("Parallel mode: {}, Threads: {}",
                                config.getParallelMode(), config.getThreadCount());
                }
        }

        @AfterSuite
        public void afterSuite() {
                logger.info("Completed API test suite");
        }
}