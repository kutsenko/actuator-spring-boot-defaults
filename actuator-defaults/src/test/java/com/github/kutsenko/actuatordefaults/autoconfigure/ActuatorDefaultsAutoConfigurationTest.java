package com.github.kutsenko.actuatordefaults.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

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
 * driver, Tomcat, Spring MVC (DispatcherServlet) and the blocking RestClient — can activate here; the
 * remaining contributors (Jetty, Reactor Netty, WebClient, Redis, Elasticsearch) are wired identically
 * by class name and their catalogues are covered by {@code PropertyTimeoutDefaultsContributorTest}.
 */
class ActuatorDefaultsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ActuatorDefaultsAutoConfiguration.class));

    @Test
    void reportsAGroupForEveryDependencyOnTheClasspath() {
        runner.run(context -> assertThat(dependencies(context))
                .contains(
                        "HikariCP (JDBC connection pool)",
                        "MongoDB driver (MongoClientSettings)",
                        "Embedded Tomcat (servlet web server)",
                        "Servlet web (Spring MVC)",
                        "Spring HTTP client (RestClient / RestTemplate)"));
    }

    @Test
    void dropsTheHikariGroupWhenHikariAbsent() {
        runner.withClassLoader(new FilteredClassLoader(HikariDataSource.class)).run(context -> {
            assertThat(context).hasSingleBean(DefaultsService.class);
            assertThat(dependencies(context)).noneMatch(d -> d.contains("HikariCP"));
        });
    }

    @Test
    void dropsTheHttpClientGroupWhenSpringWebAbsent() {
        runner.withClassLoader(new FilteredClassLoader(RestClient.class))
                .run(context -> assertThat(dependencies(context)).noneMatch(d -> d.contains("Spring HTTP client")));
    }

    @Test
    void reportsNoGroupsWhenEveryOptionalDependencyAbsent() {
        runner.withClassLoader(new FilteredClassLoader(
                        HikariDataSource.class,
                        MongoClientSettings.class,
                        Tomcat.class,
                        DispatcherServlet.class,
                        RestClient.class))
                .run(context -> assertThat(dependencies(context)).isEmpty());
    }

    private static List<String> dependencies(AssertableApplicationContext context) {
        return context.getBean(DefaultsService.class).report().groups().stream()
                .map(DefaultsGroup::dependency)
                .toList();
    }
}
