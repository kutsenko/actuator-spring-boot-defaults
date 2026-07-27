package com.github.kutsenko.actuatordefaults.autoconfigure;

import com.github.kutsenko.actuatordefaults.BootDefaultsEndpoint;
import com.github.kutsenko.actuatordefaults.DefaultsContributor;
import com.github.kutsenko.actuatordefaults.DefaultsService;
import com.github.kutsenko.actuatordefaults.contributor.HikariTimeoutDefaultsContributor;
import com.github.kutsenko.actuatordefaults.contributor.HttpClientTimeoutDefaultsContributor;
import com.github.kutsenko.actuatordefaults.contributor.MongoTimeoutDefaultsContributor;
import com.mongodb.MongoClientSettings;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration for the boot-defaults endpoint.
 *
 * <p>Always registers the {@link DefaultsService} and, when actuator endpoints are available, the
 * {@link BootDefaultsEndpoint}. The per-dependency {@link DefaultsContributor}s live in nested
 * {@code @ConditionalOnClass}-guarded configurations, so a contributor bean is created only when
 * its dependency is on the consumer's classpath — that is the mechanism behind "dependency absent
 * → no defaults determined for it". Adding support for a new dependency is a new nested block; no
 * change to the service or endpoint.
 */
@AutoConfiguration
public class ActuatorDefaultsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DefaultsService defaultsService(ObjectProvider<DefaultsContributor> contributors) {
        return new DefaultsService(contributors.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint(endpoint = BootDefaultsEndpoint.class)
    public BootDefaultsEndpoint bootDefaultsEndpoint(DefaultsService defaultsService) {
        return new BootDefaultsEndpoint(defaultsService);
    }

    /** Contributed only when HikariCP is on the classpath. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HikariDataSource.class)
    static class HikariDefaultsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        HikariTimeoutDefaultsContributor hikariTimeoutDefaultsContributor(
                ObjectProvider<HikariDataSource> dataSources) {
            return new HikariTimeoutDefaultsContributor(dataSources);
        }
    }

    /** Contributed only when the MongoDB driver is on the classpath. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MongoClientSettings.class)
    static class MongoDefaultsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        MongoTimeoutDefaultsContributor mongoTimeoutDefaultsContributor(
                ObjectProvider<MongoClientSettings> settings) {
            return new MongoTimeoutDefaultsContributor(settings);
        }
    }

    /** Contributed only when Spring's HTTP client ({@code spring-web}) is on the classpath. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClient.class)
    static class HttpClientDefaultsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        HttpClientTimeoutDefaultsContributor httpClientTimeoutDefaultsContributor(Environment environment) {
            return new HttpClientTimeoutDefaultsContributor(environment);
        }
    }
}
