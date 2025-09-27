package api.hooks;

import api.config.Configuration;
import org.testng.xml.*;

import java.util.List;

public class SuiteGenerator {

    public static XmlSuite createDynamicSuite(Configuration config) {
        XmlSuite suite = new XmlSuite();
        suite.setName("API Test Suite");

        // Set parallel execution based on configuration
        if (config.isParallelEnabled()) {
            suite.setParallel(XmlSuite.ParallelMode.valueOf(config.getParallelMode().toUpperCase()));
            suite.setThreadCount(config.getThreadCount());
        } else {
            suite.setParallel(XmlSuite.ParallelMode.NONE);
        }

        // Configure data provider threading
        suite.setDataProviderThreadCount(config.getDataProviderThreadCount());

        // Create test
        XmlTest test = new XmlTest(suite);
        test.setName("API Tests");
        test.setXmlClasses(List.of(new XmlClass(TestRunner.class)));

        return suite;
    }
}