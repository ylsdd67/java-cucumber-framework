package com.framework.core.client;

import com.framework.core.config.ConfigManager;

/**
 * Core abstraction for all protocol clients.
 * <p>
 * Each protocol (REST, SOAP, MQTT, Kafka, gRPC, etc.) implements this
 * interface. The framework routes requests through the appropriate client
 * based on protocol type.
 * <p>
 * <b>To add a new protocol:</b>
 * <ol>
 *   <li>Create a new class implementing {@code ProtocolClient}</li>
 *   <li>Register it in {@link ProtocolClientFactory}</li>
 *   <li>Add protocol-specific step definitions under a new package</li>
 * </ol>
 */
public interface ProtocolClient {

    /**
     * Initialize the client with framework configuration.
     * Called once when the client is created by the factory.
     *
     * @param config the global configuration manager
     */
    void init(ConfigManager config);

    /**
     * Execute a protocol request and return the response.
     *
     * @param request the protocol-agnostic request
     * @return the protocol-agnostic response
     */
    ProtocolResponse execute(ProtocolRequest request);

    /**
     * Return the protocol identifier this client handles.
     * Examples: "REST", "SOAP", "MQTT", "KAFKA", "GRPC"
     */
    String getProtocolName();

    /**
     * Clean up resources (close connections, sessions, etc.).
     * Called at the end of a test scenario or suite.
     */
    default void close() {
        // Default no-op; override if cleanup is needed
    }
}
