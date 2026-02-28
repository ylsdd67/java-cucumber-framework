package com.framework.core.client;

import java.util.Map;

/**
 * Generic request object that works across all protocols.
 * <p>
 * For REST/SOAP: represents an HTTP request.
 * For MQTT: represents a publish/subscribe message.
 * For Kafka: represents a producer record.
 * <p>
 * Each protocol adapter interprets the fields relevant to it.
 */
public class ProtocolRequest {

    /** The target endpoint / topic / queue / URL path */
    private String endpoint;

    /** The method or action (GET, POST, PUBLISH, PRODUCE, etc.) */
    private String method;

    /** Headers / metadata / properties */
    private final Map<String, String> headers = new java.util.LinkedHashMap<>();

    /** Query parameters (REST) or message properties (messaging) */
    private final Map<String, String> queryParams = new java.util.LinkedHashMap<>();

    /** Path parameters for URL template substitution */
    private final Map<String, String> pathParams = new java.util.LinkedHashMap<>();

    /** Request body (JSON string, XML string, binary as Base64, etc.) */
    private String body;

    /** Content type hint (application/json, text/xml, etc.) */
    private String contentType;

    /** Timeout in milliseconds; 0 means use default */
    private long timeoutMs;

    /** Optional authentication token / key */
    private String authToken;

    /** Optional basic-auth username */
    private String basicAuthUser;

    /** Optional basic-auth password */
    private String basicAuthPassword;

    /** Generic bag for protocol-specific extensions */
    private final Map<String, Object> extras = new java.util.LinkedHashMap<>();

    // ---- Fluent builder-style setters ----

    public ProtocolRequest endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ProtocolRequest method(String method) {
        this.method = method;
        return this;
    }

    public ProtocolRequest header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public ProtocolRequest headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public ProtocolRequest queryParam(String key, String value) {
        this.queryParams.put(key, value);
        return this;
    }

    public ProtocolRequest pathParam(String key, String value) {
        this.pathParams.put(key, value);
        return this;
    }

    public ProtocolRequest body(String body) {
        this.body = body;
        return this;
    }

    public ProtocolRequest contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public ProtocolRequest timeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public ProtocolRequest authToken(String token) {
        this.authToken = token;
        return this;
    }

    public ProtocolRequest basicAuth(String user, String password) {
        this.basicAuthUser = user;
        this.basicAuthPassword = password;
        return this;
    }

    public ProtocolRequest extra(String key, Object value) {
        this.extras.put(key, value);
        return this;
    }

    // ---- Getters ----

    public String getEndpoint()          { return endpoint; }
    public String getMethod()            { return method; }
    public Map<String, String> getHeaders()     { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public Map<String, String> getPathParams()  { return pathParams; }
    public String getBody()              { return body; }
    public String getContentType()       { return contentType; }
    public long getTimeoutMs()           { return timeoutMs; }
    public String getAuthToken()         { return authToken; }
    public String getBasicAuthUser()     { return basicAuthUser; }
    public String getBasicAuthPassword() { return basicAuthPassword; }
    public Map<String, Object> getExtras()      { return extras; }

    @Override
    public String toString() {
        return "ProtocolRequest{" +
                "method='" + method + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", contentType='" + contentType + '\'' +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                '}';
    }
}
