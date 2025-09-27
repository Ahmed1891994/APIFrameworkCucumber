package api.hooks;

import api.config.Configuration;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

import java.util.Collections;

public class TestExecutor {
    public static void main(String[] args) {
        String environment = args.length > 0 ? args[0] : "dev";
        Configuration config = new Configuration(environment);

        // Create dynamic suite
        XmlSuite suite = SuiteGenerator.createDynamicSuite(config);

        // Execute tests
        TestNG testNG = new TestNG();
        testNG.setXmlSuites(Collections.singletonList(suite));
        testNG.run();

        System.exit(testNG.hasFailure() ? 1 : 0);
    }
}