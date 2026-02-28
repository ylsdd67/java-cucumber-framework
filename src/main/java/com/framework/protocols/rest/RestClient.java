package com.framework.protocols.rest;

import com.framework.core.client.ProtocolClient;
import com.framework.core.client.ProtocolRequest;
import com.framework.core.client.ProtocolResponse;
import com.framework.core.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST/HTTP protocol client powered by REST Assured.
 * <p>
 * Translates a generic {@link ProtocolRequest} into REST Assured calls
 * and maps the result back to a generic {@link ProtocolResponse}.
 */
public class RestClient implements ProtocolClient {

    private static final Logger log = LoggerFactory.getLogger(RestClient.class);

    private String baseUrl;

    @Override
    public void init(ConfigManager config) {
        this.baseUrl = config.getString("rest.base-url", "http://localhost:8080");

        // Global REST Assured configuration
        RestAssured.baseURI = this.baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        boolean relaxedHttps = config.getBoolean("rest.relaxed-https", false);
        if (relaxedHttps) {
            RestAssured.useRelaxedHTTPSValidation();
        }

        int defaultTimeout = config.getInt("rest.timeout-ms", 30_000);
        RestAssured.config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", defaultTimeout)
                        .setParam("http.socket.timeout", defaultTimeout));

        log.info("REST client initialized — baseUrl={}, relaxedHttps={}, timeout={}ms",
                baseUrl, relaxedHttps, defaultTimeout);
    }

    @Override
    public ProtocolResponse execute(ProtocolRequest request) {
        log.info("Executing REST request: {}", request);

        RequestSpecification spec = RestAssured.given();

        // Headers
        if (!request.getHeaders().isEmpty()) {
            spec.headers(request.getHeaders());
        }

        // Query params
        if (!request.getQueryParams().isEmpty()) {
            spec.queryParams(request.getQueryParams());
        }

        // Path params
        if (!request.getPathParams().isEmpty()) {
            spec.pathParams(request.getPathParams());
        }

        // Content type
        if (request.getContentType() != null) {
            spec.contentType(request.getContentType());
        }

        // Authentication
        if (request.getAuthToken() != null) {
            spec.header("Authorization", "Bearer " + request.getAuthToken());
        } else if (request.getBasicAuthUser() != null) {
            spec.auth().preemptive().basic(request.getBasicAuthUser(), request.getBasicAuthPassword());
        }

        // Body
        if (request.getBody() != null) {
            spec.body(request.getBody());
        }

        // Execute based on HTTP method
        String method = request.getMethod().toUpperCase();
        String endpoint = request.getEndpoint();

        Response response = switch (method) {
            case "GET"     -> spec.get(endpoint);
            case "POST"    -> spec.post(endpoint);
            case "PUT"     -> spec.put(endpoint);
            case "PATCH"   -> spec.patch(endpoint);
            case "DELETE"  -> spec.delete(endpoint);
            case "HEAD"    -> spec.head(endpoint);
            case "OPTIONS" -> spec.options(endpoint);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        // Map to generic response
        ProtocolResponse protoResponse = new ProtocolResponse()
                .statusCode(response.getStatusCode())
                .statusLine(response.getStatusLine())
                .body(response.getBody().asString())
                .contentType(response.getContentType())
                .responseTimeMs(response.getTime());

        // Copy response headers
        response.getHeaders().forEach(h -> protoResponse.header(h.getName(), h.getValue()));

        // Store the raw REST Assured response for advanced assertions
        protoResponse.extra("rawResponse", response);

        log.info("REST response: status={}, time={}ms", response.getStatusCode(), response.getTime());
        return protoResponse;
    }

    @Override
    public String getProtocolName() {
        return "REST";
    }

    @Override
    public void close() {
        RestAssured.reset();
        log.info("REST client closed and RestAssured reset.");
    }
}
