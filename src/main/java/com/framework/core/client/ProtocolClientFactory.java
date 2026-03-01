package com.framework.core.client;

import com.framework.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that creates and caches {@link ProtocolClient} instances.
 * <p>
 * Clients are discovered via {@link ServiceLoader} (SPI) or can be
 * registered manually. This enables a true plugin architecture:
 * drop a new JAR with a ProtocolClient implementation on the classpath
 * and it will be auto-discovered.
 * <p>
 * For simpler setups, clients can also be registered programmatically
 * via {@link #register(String, Class)}.
 */
@Component
public class ProtocolClientFactory {

    private static final Logger log = LoggerFactory.getLogger(ProtocolClientFactory.class);

    private final ConfigManager config;

    /** Registered client classes by protocol name (upper-cased) */
    private final Map<String, Class<? extends ProtocolClient>> registry = new ConcurrentHashMap<>();

    /** Cache of initialized client instances */
    private final Map<String, ProtocolClient> instances = new ConcurrentHashMap<>();

    public ProtocolClientFactory(ConfigManager config) {
        this.config = config;
        discoverClients();
    }

    /**
     * Manually register a protocol client class.
     */
    public void register(String protocolName, Class<? extends ProtocolClient> clientClass) {
        registry.put(protocolName.toUpperCase(), clientClass);
        log.info("Registered protocol client: {} -> {}", protocolName, clientClass.getSimpleName());
    }

    /**
     * Get (or create) a client for the given protocol.
     *
     * @param protocolName e.g. "REST", "SOAP", "MQTT", "KAFKA"
     * @return initialized ProtocolClient
     */
    public ProtocolClient getClient(String protocolName) {
        String key = protocolName.toUpperCase();
        return instances.computeIfAbsent(key, k -> {
            Class<? extends ProtocolClient> clazz = registry.get(k);
            if (clazz == null) {
                throw new IllegalArgumentException(
                        "No ProtocolClient registered for protocol: " + protocolName +
                        ". Available: " + registry.keySet());
            }
            try {
                ProtocolClient client = clazz.getDeclaredConstructor().newInstance();
                client.init(config);
                log.info("Initialized {} client: {}", k, clazz.getSimpleName());
                return client;
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate client for " + protocolName, e);
            }
        });
    }

    /**
     * Convenience method: get the REST client.
     */
    public ProtocolClient rest() {
        return getClient("REST");
    }

    /**
     * Close all active clients. Call this at the end of the test suite.
     */
    public void closeAll() {
        instances.values().forEach(client -> {
            try {
                client.close();
                log.info("Closed {} client", client.getProtocolName());
            } catch (Exception e) {
                log.warn("Error closing {} client: {}", client.getProtocolName(), e.getMessage());
            }
        });
        instances.clear();
    }

    /**
     * Auto-discover ProtocolClient implementations via Java SPI.
     */
    private void discoverClients() {
        ServiceLoader<ProtocolClient> loader = ServiceLoader.load(ProtocolClient.class);
        for (ProtocolClient client : loader) {
            registry.put(client.getProtocolName().toUpperCase(), client.getClass());
            log.info("Auto-discovered protocol client: {} -> {}",
                    client.getProtocolName(), client.getClass().getSimpleName());
        }
    }
}
