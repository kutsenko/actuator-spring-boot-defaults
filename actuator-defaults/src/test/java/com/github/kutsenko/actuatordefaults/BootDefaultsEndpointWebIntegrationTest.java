package com.github.kutsenko.actuatordefaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

/**
 * End-to-end proof that the endpoint is discovered, exposed, and serialised to JSON in a real
 * Boot application. HikariCP, the MongoDB driver and spring-web are all on the test classpath, so
 * every contributor is active; none of them has a configured bean, so the JSON shows framework
 * defaults with {@code null} actual values.
 */
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "management.endpoints.web.exposure.include=bootdefaults")
class BootDefaultsEndpointWebIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void exposesDefaultsAsJson() {
        var response = RestClient.create()
                .get()
                .uri("http://localhost:" + port + "/actuator/bootdefaults")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).contains("json");

        var body = response.getBody();
        assertThat(body)
                .contains("\"groups\"")
                .contains("HikariCP (JDBC connection pool)")
                .contains("MongoDB driver")
                .contains("Spring HTTP client")
                .contains("Embedded Tomcat (servlet web server)")
                .contains("Servlet web (Spring MVC)")
                .contains("\"key\":\"connection-timeout\"")
                .contains("\"defaultValue\":30000")   // Hikari connection-timeout
                .contains("\"defaultValue\":1800000") // servlet session-timeout (30 min)
                .contains("\"unit\":\"ms\"")
                // Override flags present at report + group + setting level (nothing configured here).
                .contains("\"hasOverrides\":false")
                .contains("\"overriddenCount\":0")
                .contains("\"overridden\":false");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
