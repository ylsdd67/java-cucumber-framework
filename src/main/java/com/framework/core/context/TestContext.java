package com.framework.core.context;

import com.framework.core.client.ProtocolClientFactory;
import com.framework.core.client.ProtocolRequest;
import com.framework.core.client.ProtocolResponse;
import com.framework.core.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Scenario-scoped test context shared across step definitions via
 * Cucumber's PicoContainer dependency injection.
 * <p>
 * Each Cucumber scenario gets its own TestContext instance, so tests
 * are fully isolated.
 * <p>
 * This class holds:
 * <ul>
 *   <li>The current request being built</li>
 *   <li>The last response received</li>
 *   <li>Shared variables for data-driven tests</li>
 *   <li>Access to the protocol client factory</li>
 * </ul>
 */
public class TestContext {

    private final ConfigManager config;
    private final ProtocolClientFactory clientFactory;

    private ProtocolRequest currentRequest;
    private ProtocolResponse lastResponse;

    /** Shared key-value store for passing data between steps */
    private final Map<String, Object> scenarioData = new HashMap<>();

    public TestContext() {
        this.config = new ConfigManager();
        this.clientFactory = new ProtocolClientFactory(config);
    }

    // ---- Config & Factory ----

    public ConfigManager getConfig() {
        return config;
    }

    public ProtocolClientFactory getClientFactory() {
        return clientFactory;
    }

    // ---- Request ----

    /**
     * Start building a new request. Resets any previous request state.
     */
    public ProtocolRequest newRequest() {
        this.currentRequest = new ProtocolRequest();
        return this.currentRequest;
    }

    public ProtocolRequest getCurrentRequest() {
        if (currentRequest == null) {
            currentRequest = new ProtocolRequest();
        }
        return currentRequest;
    }

    // ---- Response ----

    public ProtocolResponse getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(ProtocolResponse response) {
        this.lastResponse = response;
    }

    // ---- Execute shortcut ----

    /**
     * Execute the current request using the specified protocol and store the response.
     */
    public ProtocolResponse execute(String protocol) {
        ProtocolResponse response = clientFactory.getClient(protocol).execute(currentRequest);
        this.lastResponse = response;
        return response;
    }

    /**
     * Execute the current request as REST (convenience).
     */
    public ProtocolResponse executeRest() {
        return execute("REST");
    }

    // ---- Scenario data store ----

    public void set(String key, Object value) {
        scenarioData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) scenarioData.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        @SuppressWarnings("unchecked")
        T val = (T) scenarioData.get(key);
        return val != null ? val : defaultValue;
    }

    // ---- Cleanup ----

    public void cleanup() {
        clientFactory.closeAll();
        scenarioData.clear();
        currentRequest = null;
        lastResponse = null;
    }
}
