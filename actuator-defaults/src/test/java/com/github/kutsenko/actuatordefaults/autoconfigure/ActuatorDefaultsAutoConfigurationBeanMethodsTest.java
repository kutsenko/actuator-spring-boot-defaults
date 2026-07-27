package com.github.kutsenko.actuatordefaults.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.kutsenko.actuatordefaults.autoconfigure.ActuatorDefaultsAutoConfiguration.ElasticsearchDefaultsConfiguration;
import com.github.kutsenko.actuatordefaults.autoconfigure.ActuatorDefaultsAutoConfiguration.JettyDefaultsConfiguration;
import com.github.kutsenko.actuatordefaults.autoconfigure.ActuatorDefaultsAutoConfiguration.KafkaDefaultsConfiguration;
import com.github.kutsenko.actuatordefaults.autoconfigure.ActuatorDefaultsAutoConfiguration.ReactiveHttpClientDefaultsConfiguration;
import com.github.kutsenko.actuatordefaults.autoconfigure.ActuatorDefaultsAutoConfiguration.ReactorNettyDefaultsConfiguration;
import com.github.kutsenko.actuatordefaults.autoconfigure.ActuatorDefaultsAutoConfiguration.RedisDefaultsConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

/**
 * Exercises the bean methods of the nested configurations whose guard class is not on the test
 * classpath (Jetty, Reactor Netty, reactive WebClient, Redis, Elasticsearch, Kafka). {@code
 * ActuatorDefaultsAutoConfigurationTest} already covers the wiring for the classes that <em>are</em>
 * present (Tomcat, servlet, Hikari, Mongo, blocking HTTP); this pins the remaining factory blocks
 * without dragging their libraries onto the classpath.
 */
class ActuatorDefaultsAutoConfigurationBeanMethodsTest {

    private final Environment environment = new MockEnvironment();

    @Test
    void classpathAbsentConfigurationsStillBuildTheirContributors() {
        assertThat(new JettyDefaultsConfiguration()
                .jettyTimeoutDefaultsContributor(environment).contribute().dependency())
                .contains("Jetty");
        assertThat(new ReactorNettyDefaultsConfiguration()
                .reactorNettyTimeoutDefaultsContributor(environment).contribute().dependency())
                .contains("Netty");
        assertThat(new ReactiveHttpClientDefaultsConfiguration()
                .reactiveHttpClientTimeoutDefaultsContributor(environment).contribute().dependency())
                .contains("reactive");
        assertThat(new RedisDefaultsConfiguration()
                .redisTimeoutDefaultsContributor(environment).contribute().dependency())
                .contains("Redis");
        assertThat(new ElasticsearchDefaultsConfiguration()
                .elasticsearchTimeoutDefaultsContributor(environment).contribute().dependency())
                .contains("Elasticsearch");
        assertThat(new KafkaDefaultsConfiguration()
                .kafkaTimeoutDefaultsContributor(environment).contribute().dependency())
                .contains("Kafka");
    }
}
