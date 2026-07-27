package com.github.kutsenko.actuatordefaults.autoconfigure;

import com.github.kutsenko.actuatordefaults.BootDefaultsEndpoint;
import com.github.kutsenko.actuatordefaults.DefaultsContributor;
import com.github.kutsenko.actuatordefaults.DefaultsService;
import com.github.kutsenko.actuatordefaults.contributor.HikariTimeoutDefaultsContributor;
import com.github.kutsenko.actuatordefaults.contributor.MongoTimeoutDefaultsContributor;
import com.github.kutsenko.actuatordefaults.contributor.PropertyTimeoutDefaultsContributor;
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
 * defaults determined for it".
 *
 * <p>Two contributor flavours:
 * <ul>
 *   <li><b>Typed</b> (Hikari, Mongo): read actual values off the live bean, so they guard on the
 *       class literal and read defaults from the library itself.
 *   <li><b>Property-based</b> ({@link PropertyTimeoutDefaultsContributor}): read actual values off the
 *       {@link Environment}, so they reference none of the integration's types and guard by class
 *       <em>name</em> — no compile dependency. Adding a dependency is one more nested block + one
 *       catalogue factory.
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
        PropertyTimeoutDefaultsContributor httpClientTimeoutDefaultsContributor(Environment environment) {
            return PropertyTimeoutDefaultsContributor.httpClient(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    static class ReactiveHttpClientDefaultsConfiguration {

        @Bean
        PropertyTimeoutDefaultsContributor reactiveHttpClientTimeoutDefaultsContributor(Environment environment) {
            return PropertyTimeoutDefaultsContributor.reactiveHttpClient(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
    static class TomcatDefaultsConfiguration {

        @Bean
        PropertyTimeoutDefaultsContributor tomcatTimeoutDefaultsContributor(Environment environment) {
            return PropertyTimeoutDefaultsContributor.tomcat(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.eclipse.jetty.server.Server")
    static class JettyDefaultsConfiguration {

        @Bean
        PropertyTimeoutDefaultsContributor jettyTimeoutDefaultsContributor(Environment environment) {
            return PropertyTimeoutDefaultsContributor.jetty(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "reactor.netty.http.server.HttpServer")
    static class ReactorNettyDefaultsConfiguration {

        @Bean
        PropertyTimeoutDefaultsContributor reactorNettyTimeoutDefaultsContributor(Environment environment) {
            return PropertyTimeoutDefaultsContributor.reactorNetty(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    static class ServletWebDefaultsConfiguration {

        @Bean
        PropertyTimeoutDefaultsContributor servletWebTimeoutDefaultsContributor(Environment environment) {
            return PropertyTimeoutDefaultsContributor.servletWeb(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
    static class RedisDefaultsConfiguration {

        @Bean
        PropertyTimeoutDefaultsContributor redisTimeoutDefaultsContributor(Environment environment) {
            return PropertyTimeoutDefaultsContributor.redis(environment);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.elasticsearch.client.RestClient")
    static class ElasticsearchDefaultsConfiguration {

        @Bean
        PropertyTimeoutDefaultsContributor elasticsearchTimeoutDefaultsContributor(Environment environment) {
            return PropertyTimeoutDefaultsContributor.elasticsearch(environment);
        }
    }
}
