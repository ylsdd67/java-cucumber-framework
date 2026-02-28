package com.framework.core.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic response object returned by all protocol adapters.
 * <p>
 * Fields are protocol-agnostic. Each protocol adapter populates
 * the fields that are meaningful for its protocol.
 */
public class ProtocolResponse {

    /** HTTP status code, MQTT reason code, or -1 if not applicable */
    private int statusCode = -1;

    /** Status line / reason phrase */
    private String statusLine;

    /** Response headers / metadata */
    private final Map<String, String> headers = new LinkedHashMap<>();

    /** Response body as string */
    private String body;

    /** Response time in milliseconds */
    private long responseTimeMs;

    /** Content type of the response */
    private String contentType;

    /** Generic bag for protocol-specific data */
    private final Map<String, Object> extras = new LinkedHashMap<>();

    // ---- Fluent setters ----

    public ProtocolResponse statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public ProtocolResponse statusLine(String statusLine) {
        this.statusLine = statusLine;
        return this;
    }

    public ProtocolResponse header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public ProtocolResponse body(String body) {
        this.body = body;
        return this;
    }

    public ProtocolResponse responseTimeMs(long ms) {
        this.responseTimeMs = ms;
        return this;
    }

    public ProtocolResponse contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public ProtocolResponse extra(String key, Object value) {
        this.extras.put(key, value);
        return this;
    }

    // ---- Getters ----

    public int getStatusCode()                  { return statusCode; }
    public String getStatusLine()               { return statusLine; }
    public Map<String, String> getHeaders()     { return Collections.unmodifiableMap(headers); }
    public String getBody()                     { return body; }
    public long getResponseTimeMs()             { return responseTimeMs; }
    public String getContentType()              { return contentType; }
    public Map<String, Object> getExtras()      { return Collections.unmodifiableMap(extras); }

    /**
     * Get a header value (case-insensitive lookup).
     */
    public String getHeader(String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return "ProtocolResponse{" +
                "statusCode=" + statusCode +
                ", contentType='" + contentType + '\'' +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
}
