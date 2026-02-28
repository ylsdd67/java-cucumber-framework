package com.framework.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Hierarchical configuration manager.
 * <p>
 * Loads a base {@code application.yml} then overlays an environment-specific
 * file (e.g. {@code application-dev.yml}). System properties and environment
 * variables can override any value.
 * <p>
 * Lookup order (highest priority first):
 * <ol>
 *   <li>System property ({@code -Drest.base-url=...})</li>
 *   <li>Environment variable ({@code REST_BASE_URL})</li>
 *   <li>Environment-specific YAML file</li>
 *   <li>Base YAML file</li>
 * </ol>
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final String BASE_CONFIG = "config/application.yml";

    private final Map<String, Object> properties;

    public ConfigManager() {
        this(System.getProperty("env", System.getenv().getOrDefault("ENV", "dev")));
    }

    public ConfigManager(String environment) {
        Map<String, Object> merged = new HashMap<>();

        // 1. Load base config
        merged.putAll(loadYaml(BASE_CONFIG));

        // 2. Overlay environment-specific config
        String envFile = "config/application-" + environment + ".yml";
        merged.putAll(loadYaml(envFile));

        this.properties = Collections.unmodifiableMap(merged);
        log.info("Configuration loaded for environment '{}' — {} keys", environment, properties.size());
    }

    // ---- Typed getters with defaults ----

    public String getString(String key, String defaultValue) {
        String value = resolve(key);
        return value != null ? value : defaultValue;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public int getInt(String key, int defaultValue) {
        String val = resolve(key);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = resolve(key);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        String val = resolve(key);
        return val != null ? Long.parseLong(val) : defaultValue;
    }

    // ---- Internal ----

    /**
     * Resolve a property by key, checking system props → env vars → YAML.
     */
    private String resolve(String key) {
        // System property: rest.base-url
        String sys = System.getProperty(key);
        if (sys != null) return sys;

        // Environment variable: REST_BASE_URL
        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String env = System.getenv(envKey);
        if (env != null) return env;

        // YAML (supports dot-notation flattened keys)
        Object val = properties.get(key);
        return val != null ? val.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String resource) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                log.debug("Config file not found on classpath: {}", resource);
                return Collections.emptyMap();
            }
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(is);
            if (raw == null) return Collections.emptyMap();

            // Flatten nested keys: { rest: { base-url: x } } -> { rest.base-url: x }
            Map<String, Object> flat = new HashMap<>();
            flatten("", raw, flat);
            log.debug("Loaded {} properties from {}", flat.size(), resource);
            return flat;
        } catch (Exception e) {
            log.warn("Failed to load config {}: {}", resource, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                flatten(key, (Map<String, Object>) entry.getValue(), result);
            } else {
                result.put(key, entry.getValue());
            }
        }
    }
}
