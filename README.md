# Java Cucumber Test Framework

An extensible BDD test framework built with **Java 21 + Maven + Cucumber**, designed for REST API testing with a plugin architecture that supports future protocols (SOAP, MQTT, gRPC) and middleware (Kafka, RabbitMQ).

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                   Cucumber Feature Files                 │
│              (.feature — Gherkin scenarios)               │
├──────────────────────────────────────────────────────────┤
│                   Step Definitions                        │
│         (REST steps, SOAP steps, Kafka steps...)         │
├──────────────────────────────────────────────────────────┤
│                     TestContext                           │
│        (scenario-scoped state, shared via DI)            │
├──────────────────────────────────────────────────────────┤
│               ProtocolClientFactory                      │
│           (creates clients by protocol name)             │
├────────────┬─────────────┬─────────────┬─────────────────┤
│  RestClient│  SoapClient │  MqttClient │   KafkaClient   │
│ (REST Ass.)│  (future)   │  (future)   │   (future)      │
├────────────┴─────────────┴─────────────┴─────────────────┤
│        ProtocolClient Interface (core contract)          │
│     ProtocolRequest / ProtocolResponse (generic DTOs)    │
├──────────────────────────────────────────────────────────┤
│                  ConfigManager (YAML)                    │
│        (environment profiles, system prop overrides)     │
└──────────────────────────────────────────────────────────┘
```

## Project Structure

```
src/
├── main/java/com/framework/
│   ├── core/
│   │   ├── client/        ← ProtocolClient, Request, Response, Factory
│   │   ├── config/        ← ConfigManager (YAML + env overrides)
│   │   └── context/       ← TestContext (scenario-scoped state)
│   └── protocols/
│       └── rest/          ← RestClient (REST Assured implementation)
│           # Future: soap/, mqtt/, kafka/
└── test/
    ├── java/com/framework/
    │   ├── runners/       ← CucumberRunner (JUnit 5 Suite)
    │   ├── hooks/         ← Before/After scenario hooks
    │   └── stepdefs/
    │       └── rest/      ← REST step definitions
    └── resources/
        ├── features/rest/ ← Gherkin .feature files
        └── config/        ← YAML configuration files
```

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+

### Run all REST tests

```bash
mvn clean test
```

### Run with a specific environment

```bash
mvn clean test -Denv=staging
```

### Run by protocol profile

```bash
mvn clean test -Prest        # Only @rest tagged tests (default)
mvn clean test -Pall         # All tests
mvn clean test -Pkafka       # Only @kafka tagged tests (future)
```

### Run a specific feature

```bash
mvn clean test -Dcucumber.features="src/test/resources/features/rest/sample_api.feature"
```

### Run by tag expression

```bash
mvn clean test -Dcucumber.filter.tags="@rest and not @slow"
```

## Configuration

Configuration uses YAML files with environment profiles:

| File                             | Purpose           |
| -------------------------------- | ----------------- |
| `config/application.yml`         | Base defaults     |
| `config/application-dev.yml`     | Dev overrides     |
| `config/application-staging.yml` | Staging overrides |

**Override priority** (highest first):

1. System property: `-Drest.base-url=http://...`
2. Environment variable: `REST_BASE_URL=http://...`
3. Environment-specific YAML
4. Base YAML

## Writing Tests

### Gherkin Feature File

```gherkin
@rest
Feature: User API
  Scenario: Get user by ID
    When I send a GET request to "/users/1"
    Then the response status code should be 200
    And the JSON path "$.name" should not be empty
```

### Available Step Definitions

| Step                                                                | Description        |
| ------------------------------------------------------------------- | ------------------ |
| `Given the REST API base URL is "{url}"`                            | Override base URL  |
| `Given I set header "{name}" to "{value}"`                          | Add request header |
| `Given I set query parameter "{name}" to "{value}"`                 | Add query param    |
| `Given I set the request body to: {docstring}`                      | Set JSON body      |
| `When I send a {METHOD} request to "{path}"`                        | Execute request    |
| `When I send a {METHOD} request to "{path}" with body: {docstring}` | Execute with body  |
| `Then the response status code should be {code}`                    | Assert status      |
| `Then the JSON path "{expr}" should equal "{value}"`                | Assert JSON value  |
| `Then the JSON path "{expr}" should not be empty`                   | Assert not empty   |
| `Then the response time should be less than {ms} ms`                | Assert perf        |
| `Then I store the JSON path "{expr}" as "{key}"`                    | Save for later     |

## Adding a New Protocol

1. **Create a client** — implement `ProtocolClient` in `src/main/java/com/framework/protocols/yourprotocol/`
2. **Register via SPI** — add the class name to `META-INF/services/com.framework.core.client.ProtocolClient`
3. **Add step definitions** — create a new package under `src/test/java/com/framework/stepdefs/yourprotocol/`
4. **Add features** — create `.feature` files under `src/test/resources/features/yourprotocol/`
5. **Add config** — add protocol config to `application.yml`

## Reports

After test execution, reports are generated at:

- **HTML**: `target/cucumber-reports/cucumber.html`
- **JSON**: `target/cucumber-reports/cucumber.json`
