package com.github.kutsenko.actuatordefaults;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

        assertThat(response.getStatusCode().value(), is(200));
        assertThat(response.getHeaders().getContentType(), is(notNullValue()));
        assertThat(response.getHeaders().getContentType().toString(), containsString("json"));

        assertThat(response.getBody(), allOf(
                containsString("\"groups\""),
                containsString("HikariCP (JDBC connection pool)"),
                containsString("MongoDB driver"),
                containsString("Spring HTTP client"),
                containsString("Embedded Tomcat (servlet web server)"),
                containsString("Servlet web (Spring MVC)"),
                containsString("\"key\":\"connection-timeout\""),
                containsString("\"defaultValue\":30000"),   // Hikari connection-timeout
                containsString("\"defaultValue\":1800000"), // servlet session-timeout (30 min)
                containsString("\"unit\":\"ms\""),
                // Override flags present at report + group + setting level (nothing configured here).
                containsString("\"hasOverrides\":false"),
                containsString("\"overriddenCount\":0"),
                containsString("\"overridden\":false")));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
