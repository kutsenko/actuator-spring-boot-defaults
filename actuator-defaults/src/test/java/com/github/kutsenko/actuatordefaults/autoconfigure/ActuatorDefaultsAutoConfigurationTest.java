package com.github.kutsenko.actuatordefaults.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.kutsenko.actuatordefaults.DefaultsService;
import com.github.kutsenko.actuatordefaults.contributor.HikariTimeoutDefaultsContributor;
import com.github.kutsenko.actuatordefaults.contributor.HttpClientTimeoutDefaultsContributor;
import com.github.kutsenko.actuatordefaults.contributor.MongoTimeoutDefaultsContributor;
import com.mongodb.MongoClientSettings;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class ActuatorDefaultsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ActuatorDefaultsAutoConfiguration.class));

    @Test
    void registersServiceAndEveryContributorWhenAllDependenciesPresent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DefaultsService.class);
            assertThat(context).hasSingleBean(HikariTimeoutDefaultsContributor.class);
            assertThat(context).hasSingleBean(MongoTimeoutDefaultsContributor.class);
            assertThat(context).hasSingleBean(HttpClientTimeoutDefaultsContributor.class);
        });
    }

    @Test
    void omitsHikariContributorWhenHikariAbsent() {
        runner.withClassLoader(new FilteredClassLoader(HikariDataSource.class)).run(context -> {
            assertThat(context).hasSingleBean(DefaultsService.class);
            assertThat(context).doesNotHaveBean(HikariTimeoutDefaultsContributor.class);
        });
    }

    @Test
    void omitsMongoContributorWhenDriverAbsent() {
        runner.withClassLoader(new FilteredClassLoader(MongoClientSettings.class))
                .run(context -> assertThat(context).doesNotHaveBean(MongoTimeoutDefaultsContributor.class));
    }

    @Test
    void omitsHttpClientContributorWhenSpringWebAbsent() {
        runner.withClassLoader(new FilteredClassLoader(RestClient.class))
                .run(context -> assertThat(context).doesNotHaveBean(HttpClientTimeoutDefaultsContributor.class));
    }

    @Test
    void reportsNoGroupsWhenEveryOptionalDependencyAbsent() {
        runner.withClassLoader(new FilteredClassLoader(
                        HikariDataSource.class, MongoClientSettings.class, RestClient.class))
                .run(context -> {
                    var service = context.getBean(DefaultsService.class);
                    assertThat(service.report().groups()).isEmpty();
                });
    }
}
