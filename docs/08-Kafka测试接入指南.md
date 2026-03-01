# 08 - Kafka 测试接入指南

> **目标读者**：需要在本框架中添加 Kafka 协议测试支持的开发/测试工程师。
>
> 本文档将一步一步指导你如何基于现有框架的插件架构，接入 Kafka 协议，实现 Kafka 消息的生产和消费测试。

---

## 目录

1. [前置条件](#1-前置条件)
2. [总览：需要做什么](#2-总览需要做什么)
3. [第一步：添加 Maven 依赖](#3-第一步添加-maven-依赖)
4. [第二步：添加配置项](#4-第二步添加配置项)
5. [第三步：实现 KafkaClient](#5-第三步实现-kafkaclient)
6. [第四步：注册 SPI 服务](#6-第四步注册-spi-服务)
7. [第五步：编写 Kafka 步骤定义](#7-第五步编写-kafka-步骤定义)
8. [第六步：编写 Feature 文件](#8-第六步编写-feature-文件)
9. [第七步：运行 Kafka 测试](#9-第七步运行-kafka-测试)
10. [完整文件清单](#10-完整文件清单)
11. [常见问题排查](#11-常见问题排查)

---

## 1. 前置条件

| 条件                    | 说明                                                      |
| ----------------------- | --------------------------------------------------------- |
| Kafka 集群 / 本地 Kafka | 需要一个可连接的 Kafka Broker（可用 Docker 快速启动）     |
| Java 21+                | 项目已配置 Java 21                                        |
| 了解框架基础            | 建议先阅读 `01-架构设计文档.md` 和 `07-术语与概念入门.md` |

**用 Docker 快速启动本地 Kafka（可选）：**

```bash
# docker-compose.yml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

```bash
docker-compose up -d
```

---

## 2. 总览：需要做什么

本框架采用**插件架构**，添加新协议只需以下步骤：

```
┌─────────────────────────────────────────────────────────────┐
│  添加 Kafka 支持的完整步骤                                    │
├─────────────────────────────────────────────────────────────┤
│  1. pom.xml          → 添加 kafka-clients 依赖              │
│  2. application.yml  → 添加 kafka 配置项                     │
│  3. KafkaClient.java → 实现 ProtocolClient 接口             │
│  4. SPI 文件          → 注册 KafkaClient                     │
│  5. KafkaStepDefs.java → 编写 Cucumber 步骤定义             │
│  6. *.feature        → 编写 Gherkin 测试场景                 │
│  7. 运行测试          → mvn test -Pkafka                     │
└─────────────────────────────────────────────────────────────┘
```

> **参考模板**：所有步骤均可参照已有的 REST 实现（`RestClient.java`、`RestStepDefs.java`）。

---

## 3. 第一步：添加 Maven 依赖

在 `pom.xml` 的 `<dependencies>` 区域添加 Kafka 客户端依赖：

```xml
<!-- ==================== -->
<!-- Kafka                -->
<!-- ==================== -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.7.0</version>
</dependency>
```

> **注意**：版本号请根据你的 Kafka 集群版本选择兼容的客户端版本。

---

## 4. 第二步：添加配置项

### 4.1 基础配置 `src/test/resources/config/application.yml`

取消已有的 Kafka 注释，或添加以下内容：

```yaml
kafka:
  bootstrap-servers: localhost:9092
  group-id: test-framework
  auto-offset-reset: earliest
  key-serializer: org.apache.kafka.common.serialization.StringSerializer
  value-serializer: org.apache.kafka.common.serialization.StringSerializer
  key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
  value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
  poll-timeout-ms: 10000
```

### 4.2 环境覆盖 `src/test/resources/config/application-dev.yml`

```yaml
kafka:
  bootstrap-servers: localhost:9092
```

### 4.3 其他环境（如 staging）

```yaml
kafka:
  bootstrap-servers: staging-kafka.example.com:9092
```

> 配置通过 `ConfigManager` 加载，支持系统属性和环境变量覆盖。
> 例如：`-Dkafka.bootstrap-servers=my-kafka:9092` 或环境变量 `KAFKA_BOOTSTRAP_SERVERS`。

---

## 5. 第三步：实现 KafkaClient

创建文件：`src/main/java/com/framework/protocols/kafka/KafkaClient.java`

```java
package com.framework.protocols.kafka;

import com.framework.core.client.ProtocolClient;
import com.framework.core.client.ProtocolRequest;
import com.framework.core.client.ProtocolResponse;
import com.framework.core.config.ConfigManager;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Kafka 协议客户端 — 实现 ProtocolClient 接口。
 * <p>
 * 将 ProtocolRequest 映射为 Kafka 操作：
 * <ul>
 *   <li>method = "PRODUCE" → 发送消息到指定 topic</li>
 *   <li>method = "CONSUME" → 从指定 topic 消费消息</li>
 * </ul>
 */
public class KafkaClient implements ProtocolClient {

    private static final Logger log = LoggerFactory.getLogger(KafkaClient.class);

    private KafkaProducer<String, String> producer;
    private Properties consumerBaseProps;
    private long pollTimeoutMs;

    @Override
    public void init(ConfigManager config) {
        String bootstrapServers = config.getString("kafka.bootstrap-servers", "localhost:9092");

        // ----- Producer 配置 -----
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                config.getString("kafka.key-serializer",
                        "org.apache.kafka.common.serialization.StringSerializer"));
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                config.getString("kafka.value-serializer",
                        "org.apache.kafka.common.serialization.StringSerializer"));
        this.producer = new KafkaProducer<>(producerProps);

        // ----- Consumer 基础配置（每次消费时创建新实例） -----
        consumerBaseProps = new Properties();
        consumerBaseProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerBaseProps.put(ConsumerConfig.GROUP_ID_CONFIG,
                config.getString("kafka.group-id", "test-framework"));
        consumerBaseProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                config.getString("kafka.auto-offset-reset", "earliest"));
        consumerBaseProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                config.getString("kafka.key-deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer"));
        consumerBaseProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                config.getString("kafka.value-deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer"));

        this.pollTimeoutMs = config.getLong("kafka.poll-timeout-ms", 10_000);

        log.info("Kafka client initialized — bootstrapServers={}", bootstrapServers);
    }

    @Override
    public ProtocolResponse execute(ProtocolRequest request) {
        String method = request.getMethod().toUpperCase();
        return switch (method) {
            case "PRODUCE" -> produce(request);
            case "CONSUME" -> consume(request);
            default -> throw new IllegalArgumentException(
                    "Unsupported Kafka method: " + method + ". Use PRODUCE or CONSUME.");
        };
    }

    /**
     * 发送消息到 Kafka topic。
     * <p>
     * request.getEndpoint() → topic 名称
     * request.getBody()     → 消息内容
     * request.getHeaders().get("key") → 消息 key（可选）
     */
    private ProtocolResponse produce(ProtocolRequest request) {
        String topic = request.getEndpoint();
        String key = request.getHeaders().get("key");
        String value = request.getBody();

        log.info("Producing message to topic '{}', key='{}', bodyLength={}",
                topic, key, value != null ? value.length() : 0);

        try {
            long startTime = System.currentTimeMillis();
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

            // 同步发送以获取 RecordMetadata
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();

            long elapsed = System.currentTimeMillis() - startTime;

            ProtocolResponse response = new ProtocolResponse()
                    .statusCode(0)  // 0 表示成功
                    .body(String.format(
                            "{\"topic\":\"%s\",\"partition\":%d,\"offset\":%d}",
                            metadata.topic(), metadata.partition(), metadata.offset()))
                    .responseTimeMs(elapsed)
                    .extra("metadata", metadata);

            log.info("Message produced — topic={}, partition={}, offset={}, time={}ms",
                    metadata.topic(), metadata.partition(), metadata.offset(), elapsed);
            return response;

        } catch (Exception e) {
            log.error("Failed to produce message to topic '{}': {}", topic, e.getMessage());
            return new ProtocolResponse()
                    .statusCode(-1)
                    .body("{\"error\":\"" + e.getMessage() + "\"}")
                    .extra("exception", e);
        }
    }

    /**
     * 从 Kafka topic 消费消息。
     * <p>
     * request.getEndpoint() → topic 名称
     * request.getExtras().get("maxRecords") → 最大消费条数（默认 1）
     */
    private ProtocolResponse consume(ProtocolRequest request) {
        String topic = request.getEndpoint();
        int maxRecords = 1;
        Object maxObj = request.getExtras().get("maxRecords");
        if (maxObj instanceof Number n) {
            maxRecords = n.intValue();
        }

        log.info("Consuming from topic '{}', maxRecords={}", topic, maxRecords);

        // 每次消费创建新的 consumer（隔离测试场景）
        Properties props = new Properties();
        props.putAll(consumerBaseProps);
        // 使用唯一 group-id 避免场景间干扰
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                consumerBaseProps.getProperty(ConsumerConfig.GROUP_ID_CONFIG)
                        + "-" + System.currentTimeMillis());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));

            long startTime = System.currentTimeMillis();
            StringBuilder messages = new StringBuilder("[");
            int count = 0;

            // 轮询直到获取足够消息或超时
            long deadline = startTime + pollTimeoutMs;
            while (count < maxRecords && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofMillis(Math.min(1000, deadline - System.currentTimeMillis())));
                for (ConsumerRecord<String, String> record : records) {
                    if (count > 0) messages.append(",");
                    messages.append(String.format(
                            "{\"key\":%s,\"value\":%s,\"partition\":%d,\"offset\":%d}",
                            record.key() != null ? "\"" + record.key() + "\"" : "null",
                            record.value() != null ? "\"" + escapeJson(record.value()) + "\"" : "null",
                            record.partition(), record.offset()));
                    count++;
                    if (count >= maxRecords) break;
                }
            }
            messages.append("]");

            long elapsed = System.currentTimeMillis() - startTime;

            ProtocolResponse response = new ProtocolResponse()
                    .statusCode(count > 0 ? 0 : -1)
                    .body(messages.toString())
                    .responseTimeMs(elapsed)
                    .extra("recordCount", count);

            log.info("Consumed {} records from topic '{}', time={}ms", count, topic, elapsed);
            return response;

        } catch (Exception e) {
            log.error("Failed to consume from topic '{}': {}", topic, e.getMessage());
            return new ProtocolResponse()
                    .statusCode(-1)
                    .body("{\"error\":\"" + e.getMessage() + "\"}")
                    .extra("exception", e);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public String getProtocolName() {
        return "KAFKA";
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.close();
            log.info("Kafka producer closed.");
        }
    }
}
```

### 关键设计说明

| 映射       | ProtocolRequest 字段                    | Kafka 概念             |
| ---------- | --------------------------------------- | ---------------------- |
| Topic      | `request.getEndpoint()`                 | Kafka topic 名称       |
| 操作类型   | `request.getMethod()`                   | `PRODUCE` 或 `CONSUME` |
| 消息体     | `request.getBody()`                     | 消息的 value           |
| 消息 Key   | `request.getHeaders().get("key")`       | 消息的 key             |
| 最大消费数 | `request.getExtras().get("maxRecords")` | 消费时的最大条数       |

---

## 6. 第四步：注册 SPI 服务

编辑文件：`src/main/resources/META-INF/services/com.framework.core.client.ProtocolClient`

在已有的 `RestClient` 下方**新增一行**：

```
com.framework.protocols.rest.RestClient
com.framework.protocols.kafka.KafkaClient
```

> 这就是 Java SPI 机制 —— `ProtocolClientFactory` 会在启动时自动发现并注册 `KafkaClient`。

---

## 7. 第五步：编写 Kafka 步骤定义

创建文件：`src/test/java/com/framework/stepdefs/kafka/KafkaStepDefs.java`

```java
package com.framework.stepdefs.kafka;

import com.framework.core.context.TestContext;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber 步骤定义 — Kafka 消息测试。
 */
public class KafkaStepDefs {

    private static final Logger log = LoggerFactory.getLogger(KafkaStepDefs.class);

    private final TestContext context;

    public KafkaStepDefs(TestContext context) {
        this.context = context;
    }

    // ===================================================================
    // GIVEN — 设置
    // ===================================================================

    @Given("the Kafka topic is {string}")
    public void setTopic(String topic) {
        context.getCurrentRequest().endpoint(topic);
    }

    @Given("the Kafka message key is {string}")
    public void setMessageKey(String key) {
        context.getCurrentRequest().header("key", key);
    }

    @Given("the Kafka message body is:")
    public void setMessageBody(String body) {
        context.getCurrentRequest().body(body);
    }

    @Given("the Kafka message body is {string}")
    public void setMessageBodyInline(String body) {
        context.getCurrentRequest().body(body);
    }

    // ===================================================================
    // WHEN — 执行
    // ===================================================================

    @When("I produce a message to Kafka topic {string}")
    public void produceToTopic(String topic) {
        context.getCurrentRequest()
                .method("PRODUCE")
                .endpoint(topic);
        context.execute("KAFKA");
    }

    @When("I produce a message to Kafka topic {string} with body:")
    public void produceToTopicWithBody(String topic, String body) {
        context.getCurrentRequest()
                .method("PRODUCE")
                .endpoint(topic)
                .body(body);
        context.execute("KAFKA");
    }

    @When("I produce a message to Kafka topic {string} with key {string} and body:")
    public void produceToTopicWithKeyAndBody(String topic, String key, String body) {
        context.getCurrentRequest()
                .method("PRODUCE")
                .endpoint(topic)
                .body(body);
        context.getCurrentRequest().header("key", key);
        context.execute("KAFKA");
    }

    @When("I consume {int} message(s) from Kafka topic {string}")
    public void consumeFromTopic(int count, String topic) {
        context.getCurrentRequest()
                .method("CONSUME")
                .endpoint(topic)
                .extra("maxRecords", count);
        context.execute("KAFKA");
    }

    @When("I consume a message from Kafka topic {string}")
    public void consumeOneFromTopic(String topic) {
        consumeFromTopic(1, topic);
    }

    // ===================================================================
    // THEN — 断言
    // ===================================================================

    @Then("the Kafka operation should succeed")
    public void verifySuccess() {
        assertThat(context.getLastResponse().getStatusCode())
                .as("Kafka operation status (0 = success)")
                .isEqualTo(0);
    }

    @Then("the Kafka operation should fail")
    public void verifyFailure() {
        assertThat(context.getLastResponse().getStatusCode())
                .as("Kafka operation status (-1 = failure)")
                .isEqualTo(-1);
    }

    @Then("the Kafka response should contain {string}")
    public void responseContains(String expected) {
        assertThat(context.getLastResponse().getBody())
                .as("Kafka response body should contain '%s'", expected)
                .contains(expected);
    }

    @Then("the consumed record count should be {int}")
    public void verifyRecordCount(int expected) {
        Object count = context.getLastResponse().getExtras().get("recordCount");
        assertThat(((Number) count).intValue())
                .as("Consumed record count")
                .isEqualTo(expected);
    }

    @Then("the consumed message value should contain {string}")
    public void consumedValueContains(String expected) {
        String body = context.getLastResponse().getBody();
        assertThat(body)
                .as("Consumed message value should contain '%s'", expected)
                .contains(expected);
    }

    @Then("I store the Kafka response as {string}")
    public void storeResponse(String key) {
        context.set(key, context.getLastResponse().getBody());
        log.info("Stored Kafka response as '{}'", key);
    }
}
```

---

## 8. 第六步：编写 Feature 文件

创建文件：`src/test/resources/features/kafka/kafka_messaging.feature`

```gherkin
@kafka
Feature: Kafka 消息测试
  作为测试工程师
  我需要验证 Kafka 消息的生产和消费功能
  以确保消息系统工作正常

  # -------------------------------------------------------
  # 生产消息
  # -------------------------------------------------------

  Scenario: 发送一条消息到 Kafka topic
    When I produce a message to Kafka topic "test-topic" with body:
      """
      {
        "event": "user_created",
        "userId": 12345,
        "name": "张三"
      }
      """
    Then the Kafka operation should succeed
    And the Kafka response should contain "test-topic"

  Scenario: 发送带 key 的消息到 Kafka topic
    When I produce a message to Kafka topic "test-topic" with key "user-123" and body:
      """
      {
        "event": "user_updated",
        "userId": 123,
        "action": "update_profile"
      }
      """
    Then the Kafka operation should succeed

  # -------------------------------------------------------
  # 消费消息
  # -------------------------------------------------------

  Scenario: 从 Kafka topic 消费消息
    # 先生产一条消息
    When I produce a message to Kafka topic "test-consume-topic" with body:
      """
      {"message": "hello kafka"}
      """
    Then the Kafka operation should succeed
    # 再消费这条消息
    When I consume a message from Kafka topic "test-consume-topic"
    Then the Kafka operation should succeed
    And the consumed message value should contain "hello kafka"

  # -------------------------------------------------------
  # 链式操作：生产后消费验证
  # -------------------------------------------------------

  Scenario: 生产消息并立即消费验证
    Given the Kafka message key is "order-001"
    When I produce a message to Kafka topic "order-events" with body:
      """
      {
        "orderId": "ORD-001",
        "status": "created",
        "amount": 99.99
      }
      """
    Then the Kafka operation should succeed
    When I consume 1 message(s) from Kafka topic "order-events"
    Then the consumed message value should contain "ORD-001"
```

---

## 9. 第七步：运行 Kafka 测试

### 仅运行 Kafka 测试

```bash
# 使用已配置的 Maven profile
./mvnw test -Pkafka

# Windows
mvnw.cmd test -Pkafka
```

### 运行所有协议测试

```bash
./mvnw test -Pall
```

### 指定 Kafka 地址运行

```bash
./mvnw test -Pkafka -Dkafka.bootstrap-servers=192.168.1.100:9092
```

### 指定环境运行

```bash
./mvnw test -Pkafka -Pstaging
```

---

## 10. 完整文件清单

实现 Kafka 测试支持需要**新建/修改**的所有文件：

```
java-cucumber-framework/
├── pom.xml                                                    # [修改] 添加 kafka-clients 依赖
├── src/
│   ├── main/
│   │   ├── java/com/framework/protocols/kafka/
│   │   │   └── KafkaClient.java                               # [新建] Kafka 协议客户端
│   │   └── resources/META-INF/services/
│   │       └── com.framework.core.client.ProtocolClient       # [修改] 添加 KafkaClient 注册
│   └── test/
│       ├── java/com/framework/stepdefs/kafka/
│       │   └── KafkaStepDefs.java                             # [新建] Kafka 步骤定义
│       └── resources/
│           ├── config/
│           │   ├── application.yml                            # [修改] 添加 kafka 配置项
│           │   └── application-dev.yml                        # [修改] 添加 kafka 环境配置
│           └── features/kafka/
│               └── kafka_messaging.feature                    # [新建] Kafka 测试场景
```

> **无需修改的文件**：`CucumberRunner.java`（已配置扫描 `com.framework.stepdefs` 包下所有步骤定义）、`CucumberSpringConfig.java`（已配置扫描 `com.framework` 包）、`Hooks.java`（通用钩子适用于所有协议）。

---

## 11. 常见问题排查

### Q: 运行测试时提示 "No ProtocolClient registered for protocol: KAFKA"

**原因**：SPI 注册文件未更新。

**解决**：确认 `src/main/resources/META-INF/services/com.framework.core.client.ProtocolClient` 中包含：

```
com.framework.protocols.kafka.KafkaClient
```

然后执行 `mvn clean compile` 重新编译。

---

### Q: 连接 Kafka 超时

**原因**：Kafka Broker 地址不可达。

**排查步骤**：

1. 确认 Kafka 正在运行：`docker ps` 或 `kafka-broker-api-versions.sh --bootstrap-server localhost:9092`
2. 检查 `application.yml` 中的 `kafka.bootstrap-servers` 是否正确
3. 检查防火墙和网络配置

---

### Q: 消费不到消息

**可能原因**：

- **消息已过期**：Kafka 默认保留 7 天，检查 topic 的 `retention.ms` 配置
- **consumer group offset**：框架默认使用 `earliest`，但如果 group 之前已消费过，会从上次 offset 开始。框架每次消费使用唯一 group-id 来避免此问题
- **poll 超时时间太短**：增大 `kafka.poll-timeout-ms` 配置值

---

### Q: 测试之间数据互相干扰

**框架已处理**：

- `TestContext` 标注了 `@ScenarioScope`，每个场景有独立的上下文
- `KafkaClient` 每次消费时创建独立的 Consumer 实例，使用唯一 group-id
- `Hooks.java` 中 `@After` 会调用 `context.cleanup()` 清理状态

---

### Q: 如何查看 Kafka 客户端的详细日志

在 `src/test/resources/logback-test.xml` 中添加：

```xml
<logger name="com.framework.protocols.kafka" level="DEBUG" />
<logger name="org.apache.kafka" level="WARN" />
```

---

> **完成以上步骤后，你的框架就支持 Kafka 消息测试了！** 如有疑问，请参考已有的 REST 实现作为模板。
