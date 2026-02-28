package com.framework.runners;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * JUnit 5 + Cucumber Platform Engine runner.
 * <p>
 * Configuration can be overridden in {@code cucumber.properties}
 * or via Maven surefire {@code configurationParameters}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.framework.stepdefs,com.framework.hooks")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,html:target/cucumber-reports/cucumber.html,json:target/cucumber-reports/cucumber.json")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
public class CucumberRunner {
    // This class is intentionally empty.
    // Cucumber discovers features and glue code via the annotations above.
}
