package com.framework.stepdefs.rest;

import com.framework.core.context.TestContext;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable Cucumber step definitions for REST API testing.
 * <p>
 * These steps are protocol-aware but expressed in business-friendly
 * Gherkin language. They delegate to the generic ProtocolClient via
 * the TestContext.
 */
public class RestStepDefs {

    private static final Logger log = LoggerFactory.getLogger(RestStepDefs.class);

    private final TestContext context;

    public RestStepDefs(TestContext context) {
        this.context = context;
    }

    // ===================================================================
    // GIVEN — Setup
    // ===================================================================

    @Given("the REST API base URL is {string}")
    public void setBaseUrl(String baseUrl) {
        context.getConfig(); // config already loaded
        context.set("rest.base-url.override", baseUrl);
        io.restassured.RestAssured.baseURI = baseUrl;
        log.info("Base URL overridden to: {}", baseUrl);
    }

    @Given("I set header {string} to {string}")
    public void setHeader(String name, String value) {
        context.getCurrentRequest().header(name, value);
    }

    @Given("I set the following headers:")
    public void setHeaders(DataTable table) {
        Map<String, String> headers = table.asMap(String.class, String.class);
        context.getCurrentRequest().headers(headers);
    }

    @Given("I set query parameter {string} to {string}")
    public void setQueryParam(String name, String value) {
        context.getCurrentRequest().queryParam(name, value);
    }

    @Given("I set path parameter {string} to {string}")
    public void setPathParam(String name, String value) {
        context.getCurrentRequest().pathParam(name, value);
    }

    @Given("I set request content type to {string}")
    public void setContentType(String contentType) {
        context.getCurrentRequest().contentType(contentType);
    }

    @Given("I set bearer token {string}")
    public void setBearerToken(String token) {
        context.getCurrentRequest().authToken(token);
    }

    @Given("I set basic auth with username {string} and password {string}")
    public void setBasicAuth(String user, String password) {
        context.getCurrentRequest().basicAuth(user, password);
    }

    @Given("I set the request body to:")
    public void setRequestBody(String body) {
        context.getCurrentRequest().body(body);
        if (context.getCurrentRequest().getContentType() == null) {
            context.getCurrentRequest().contentType("application/json");
        }
    }

    @Given("I set the request body from file {string}")
    public void setRequestBodyFromFile(String filePath) {
        try {
            var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
            if (is == null) throw new IllegalArgumentException("File not found on classpath: " + filePath);
            String body = new String(is.readAllBytes());
            context.getCurrentRequest().body(body);
            if (context.getCurrentRequest().getContentType() == null) {
                context.getCurrentRequest().contentType("application/json");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read request body file: " + filePath, e);
        }
    }

    @Given("I store {string} as {string}")
    public void storeValue(String value, String key) {
        context.set(key, value);
    }

    // ===================================================================
    // WHEN — Execute
    // ===================================================================

    @When("I send a {word} request to {string}")
    public void sendRequest(String method, String endpoint) {
        context.getCurrentRequest()
                .method(method)
                .endpoint(endpoint);
        context.executeRest();
    }

    @When("I send a {word} request to {string} with body:")
    public void sendRequestWithBody(String method, String endpoint, String body) {
        context.getCurrentRequest()
                .method(method)
                .endpoint(endpoint)
                .body(body);
        if (context.getCurrentRequest().getContentType() == null) {
            context.getCurrentRequest().contentType("application/json");
        }
        context.executeRest();
    }

    // ===================================================================
    // THEN — Assertions
    // ===================================================================

    @Then("the response status code should be {int}")
    public void verifyStatusCode(int expectedStatus) {
        assertThat(context.getLastResponse().getStatusCode())
                .as("HTTP status code")
                .isEqualTo(expectedStatus);
    }

    @Then("the response status code should be one of {string}")
    public void verifyStatusCodeOneOf(String codes) {
        int[] expected = java.util.Arrays.stream(codes.split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
        assertThat(context.getLastResponse().getStatusCode())
                .as("HTTP status code should be one of: %s", codes)
                .isIn(java.util.Arrays.stream(expected).boxed().toArray());
    }

    @Then("the response body should contain {string}")
    public void bodyContains(String expected) {
        assertThat(context.getLastResponse().getBody())
                .as("Response body should contain '%s'", expected)
                .contains(expected);
    }

    @Then("the response body should not contain {string}")
    public void bodyNotContains(String unexpected) {
        assertThat(context.getLastResponse().getBody())
                .as("Response body should NOT contain '%s'", unexpected)
                .doesNotContain(unexpected);
    }

    @Then("the response header {string} should be {string}")
    public void verifyHeader(String headerName, String expectedValue) {
        String actual = context.getLastResponse().getHeader(headerName);
        assertThat(actual)
                .as("Response header '%s'", headerName)
                .isEqualTo(expectedValue);
    }

    @Then("the response header {string} should contain {string}")
    public void verifyHeaderContains(String headerName, String expectedSubstring) {
        String actual = context.getLastResponse().getHeader(headerName);
        assertThat(actual)
                .as("Response header '%s' should contain '%s'", headerName, expectedSubstring)
                .contains(expectedSubstring);
    }

    @Then("the response time should be less than {long} ms")
    public void verifyResponseTime(long maxMs) {
        assertThat(context.getLastResponse().getResponseTimeMs())
                .as("Response time in milliseconds")
                .isLessThan(maxMs);
    }

    @Then("the response content type should be {string}")
    public void verifyContentType(String expected) {
        assertThat(context.getLastResponse().getContentType())
                .as("Response content type")
                .containsIgnoringCase(expected);
    }

    // ---- JSON Path assertions ----

    @Then("the JSON path {string} should equal {string}")
    public void jsonPathEquals(String path, String expected) {
        String body = context.getLastResponse().getBody();
        Object actual = JsonPath.read(body, path);
        assertThat(String.valueOf(actual))
                .as("JSON path '%s'", path)
                .isEqualTo(expected);
    }

    @Then("the JSON path {string} should equal {int}")
    public void jsonPathEqualsInt(String path, int expected) {
        String body = context.getLastResponse().getBody();
        Object actual = JsonPath.read(body, path);
        assertThat(((Number) actual).intValue())
                .as("JSON path '%s'", path)
                .isEqualTo(expected);
    }

    @Then("the JSON path {string} should not be empty")
    public void jsonPathNotEmpty(String path) {
        String body = context.getLastResponse().getBody();
        Object actual = JsonPath.read(body, path);
        assertThat(actual)
                .as("JSON path '%s' should not be null/empty", path)
                .isNotNull();
        if (actual instanceof String s) {
            assertThat(s).isNotEmpty();
        } else if (actual instanceof java.util.Collection<?> c) {
            assertThat(c).isNotEmpty();
        }
    }

    @Then("the JSON path {string} should have {int} items")
    public void jsonPathArraySize(String path, int expectedSize) {
        String body = context.getLastResponse().getBody();
        java.util.List<?> list = JsonPath.read(body, path);
        assertThat(list)
                .as("JSON path '%s' array size", path)
                .hasSize(expectedSize);
    }

    @Then("the JSON path {string} should contain {string}")
    public void jsonPathContains(String path, String expected) {
        String body = context.getLastResponse().getBody();
        Object actual = JsonPath.read(body, path);
        assertThat(String.valueOf(actual))
                .as("JSON path '%s' should contain '%s'", path, expected)
                .contains(expected);
    }

    // ---- Store response data for later steps ----

    @Then("I store the JSON path {string} as {string}")
    public void storeJsonPath(String path, String key) {
        String body = context.getLastResponse().getBody();
        Object value = JsonPath.read(body, path);
        context.set(key, value);
        log.info("Stored JSON path '{}' = '{}' as '{}'", path, value, key);
    }

    @Then("I store the response header {string} as {string}")
    public void storeResponseHeader(String headerName, String key) {
        String value = context.getLastResponse().getHeader(headerName);
        context.set(key, value);
        log.info("Stored header '{}' = '{}' as '{}'", headerName, value, key);
    }

    // ---- Response body schema / structure ----

    @Then("the response body should be valid JSON")
    public void responseIsValidJson() {
        String body = context.getLastResponse().getBody();
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        } catch (Exception e) {
            throw new AssertionError("Response body is not valid JSON: " + e.getMessage());
        }
    }

    @Then("I print the response body")
    public void printResponseBody() {
        log.info("Response body:\n{}", context.getLastResponse().getBody());
    }
}
