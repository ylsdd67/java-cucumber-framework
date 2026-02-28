package com.framework.hooks;

import com.framework.core.context.TestContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber hooks that run before/after each scenario.
 * <p>
 * TestContext is injected by PicoContainer — each scenario
 * gets a fresh instance automatically.
 */
public class Hooks {

    private static final Logger log = LoggerFactory.getLogger(Hooks.class);

    private final TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        log.info("========== SCENARIO START: {} ==========", scenario.getName());
        log.info("Tags: {}", scenario.getSourceTagNames());
    }

    @After
    public void afterScenario(Scenario scenario) {
        // Attach response body to report if scenario failed
        if (scenario.isFailed() && context.getLastResponse() != null) {
            String responseBody = context.getLastResponse().getBody();
            if (responseBody != null) {
                scenario.attach(responseBody, "text/plain", "Last Response Body");
            }
        }

        // Log result
        log.info("========== SCENARIO END: {} — {} ==========",
                scenario.getName(), scenario.getStatus());

        // Cleanup
        context.cleanup();
    }
}
