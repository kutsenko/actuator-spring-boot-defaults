package com.github.kutsenko.actuatordefaults.autoconfigure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

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
        var jetty = new JettyDefaultsConfiguration();
        assertThat(jetty.jettyTimeoutDefaultsContributor(environment).contribute().dependency(),
                containsString("Jetty"));
        assertThat(jetty.jettyIoDefaultsContributor(environment).contribute().category(), is("io"));

        var netty = new ReactorNettyDefaultsConfiguration();
        assertThat(netty.reactorNettyTimeoutDefaultsContributor(environment).contribute().dependency(),
                containsString("Netty"));
        assertThat(netty.reactorNettyIoDefaultsContributor(environment).contribute().category(), is("io"));

        var reactive = new ReactiveHttpClientDefaultsConfiguration();
        assertThat(reactive.reactiveHttpClientTimeoutDefaultsContributor(environment).contribute().dependency(),
                containsString("reactive"));
        assertThat(reactive.webfluxMultipartIoDefaultsContributor(environment).contribute().dependency(),
                containsString("WebFlux"));
        assertThat(new RedisDefaultsConfiguration()
                .redisTimeoutDefaultsContributor(environment).contribute().dependency(), containsString("Redis"));
        assertThat(new ElasticsearchDefaultsConfiguration()
                .elasticsearchTimeoutDefaultsContributor(environment).contribute().dependency(),
                containsString("Elasticsearch"));
        assertThat(new KafkaDefaultsConfiguration()
                .kafkaTimeoutDefaultsContributor(environment).contribute().dependency(), containsString("Kafka"));
    }
}
