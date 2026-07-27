package com.github.kutsenko.actuatordefaults.autoconfigure;

import com.github.kutsenko.actuatordefaults.BootDefaultsEndpoint;
import com.github.kutsenko.actuatordefaults.DefaultsContributor;
import com.github.kutsenko.actuatordefaults.DefaultsService;
import com.github.kutsenko.actuatordefaults.contributor.HikariTimeoutDefaultsContributor;
import com.github.kutsenko.actuatordefaults.contributor.MongoTimeoutDefaultsContributor;
import com.github.kutsenko.actuatordefaults.contributor.PropertyDefaultsContributor;
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

/**
 * Auto-configuration for the {@code bootdefaults} endpoint.
 *
 * <p>Always registers the {@link DefaultsService} and, when actuator endpoints are available, the
 * {@link BootDefaultsEndpoint}. The per-dependency {@link DefaultsContributor}s live in nested
 * {@code @ConditionalOnClass}-guarded configurations, so a contributor bean is created only when its
 * dependency is on the consumer's classpath — that is the mechanism behind "dependency absent → no
 * defaults determined for it". A dependency may contribute more than one group (e.g. Tomcat reports
 * both a {@code timeouts} and an {@code io} group).
 *
 * <p>Two contributor flavours:
 * <ul>
 *   <li><b>Typed</b> (Hikari, Mongo): read actual values off the live bean, guard on the class
 *       literal, read defaults from the library itself.
 *   <li><b>Property-based</b> ({@link PropertyDefaultsContributor}): read actual values off the
 *       {@link Environment}, reference none of the integration's types, and guard by class
 *       <em>name</em> — no compile dependency.
 * </ul>
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

    // ---- typed contributors -----------------------------------------------------------

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

    // ---- property-based contributors (guarded by class name — no compile dependency) --
    // All produce the same bean type, so each carries a distinct bean name and no
    // @ConditionalOnMissingBean(type) — that would collapse them to a single bean.

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.client.RestClient")
    static class HttpClientDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor httpClientTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.httpClient(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    static class ReactiveHttpClientDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor reactiveHttpClientTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.reactiveHttpClient(environment);
        }

        @Bean
        PropertyDefaultsContributor webfluxMultipartIoDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.webfluxMultipart(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
    static class TomcatDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor tomcatTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.tomcat(environment);
        }

        @Bean
        PropertyDefaultsContributor tomcatIoDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.tomcatIo(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.eclipse.jetty.server.Server")
    static class JettyDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor jettyTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.jetty(environment);
        }

        @Bean
        PropertyDefaultsContributor jettyIoDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.jettyIo(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "reactor.netty.http.server.HttpServer")
    static class ReactorNettyDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor reactorNettyTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.reactorNetty(environment);
        }

        @Bean
        PropertyDefaultsContributor reactorNettyIoDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.reactorNettyIo(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    static class ServletWebDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor servletWebTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.servletWeb(environment);
        }

        @Bean
        PropertyDefaultsContributor servletMultipartIoDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.servletMultipart(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
    static class RedisDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor redisTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.redis(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.elasticsearch.client.RestClient")
    static class ElasticsearchDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor elasticsearchTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.elasticsearch(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    static class KafkaDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor kafkaTimeoutDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.kafka(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "ch.qos.logback.classic.LoggerContext")
    static class LogbackDefaultsConfiguration {

        @Bean
        PropertyDefaultsContributor logbackIoDefaultsContributor(Environment environment) {
            return PropertyDefaultsContributor.logback(environment);
        }
    }
}
