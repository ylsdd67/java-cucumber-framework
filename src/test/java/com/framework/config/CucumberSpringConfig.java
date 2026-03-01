package com.framework.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Bridges Cucumber lifecycle with Spring Boot's ApplicationContext.
 * <p>
 * {@code @CucumberContextConfiguration} tells Cucumber to use Spring
 * for dependency injection instead of PicoContainer.
 * <p>
 * The {@link TestFrameworkConfig} inner class defines the Spring context:
 * it scans {@code com.framework} to discover all {@code @Component} beans.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = CucumberSpringConfig.TestFrameworkConfig.class)
public class CucumberSpringConfig {

    @Configuration
    @ComponentScan(basePackages = "com.framework")
    public static class TestFrameworkConfig {
        // Spring scans com.framework.* and registers all @Component beans
    }
}
