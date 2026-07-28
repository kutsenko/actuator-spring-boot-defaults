package com.github.kutsenko.actuatordefaults.autoconfigure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import ch.qos.logback.classic.LoggerContext;
import com.github.kutsenko.actuatordefaults.DefaultsGroup;
import com.github.kutsenko.actuatordefaults.DefaultsService;
import com.mongodb.MongoClientSettings;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Proves the conditional wiring. Only the dependencies on the test classpath — HikariCP, the MongoDB
 * driver, Tomcat, Spring MVC (DispatcherServlet), Logback and the blocking RestClient — can activate
 * here; the remaining contributors (Jetty, Reactor Netty, WebClient, Redis, Elasticsearch, Kafka) are
 * wired identically by class name and their catalogues are covered by {@code PropertyDefaultsContributorTest}.
 */
class ActuatorDefaultsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ActuatorDefaultsAutoConfiguration.class));

    @Test
    void reportsAGroupForEveryDependencyOnTheClasspath() {
        runner.run(context -> assertThat(dependencies(context), hasItems(
                "HikariCP (JDBC connection pool)",
                "MongoDB driver (MongoClientSettings)",
                "Embedded Tomcat (servlet web server)",
                "Embedded Tomcat (request/response size limits)", // io group
                "Servlet web (Spring MVC)",
                "Servlet multipart (file uploads)",               // io group
                "Logback rolling log files",                      // io group
                "Spring HTTP client (RestClient / RestTemplate)")));
    }

    @Test
    void dropsTheHikariGroupWhenHikariAbsent() {
        runner.withClassLoader(new FilteredClassLoader(HikariDataSource.class)).run(context -> {
            assertThat(context.getBeanNamesForType(DefaultsService.class), arrayWithSize(1));
            assertThat(dependencies(context), not(hasItem(containsString("HikariCP"))));
        });
    }

    @Test
    void dropsTheHttpClientGroupWhenSpringWebAbsent() {
        runner.withClassLoader(new FilteredClassLoader(RestClient.class))
                .run(context -> assertThat(dependencies(context), not(hasItem(containsString("Spring HTTP client")))));
    }

    @Test
    void reportsNoGroupsWhenEveryOptionalDependencyAbsent() {
        runner.withClassLoader(new FilteredClassLoader(
                        HikariDataSource.class,
                        MongoClientSettings.class,
                        Tomcat.class,
                        DispatcherServlet.class,
                        RestClient.class,
                        LoggerContext.class))
                .run(context -> assertThat(dependencies(context), is(empty())));
    }

    private static List<String> dependencies(AssertableApplicationContext context) {
        return context.getBean(DefaultsService.class).report().groups().stream()
                .map(DefaultsGroup::dependency)
                .toList();
    }
}
