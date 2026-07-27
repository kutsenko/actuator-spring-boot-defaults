package com.github.kutsenko.actuatordefaults.contributor;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.github.kutsenko.actuatordefaults.DefaultsGroup;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class PropertyTimeoutDefaultsContributorTest {

    // ---- generic behaviour --------------------------------------------------

    @Test
    void parsesConfiguredDurationAndFlagsOverrideAgainstUnsetDefault() {
        var environment = new MockEnvironment().withProperty("server.tomcat.connection-timeout", "90s");

        var connection = setting(PropertyTimeoutDefaultsContributor.tomcat(environment), "connection-timeout");

        assertThat(connection.defaultValue()).isNull();          // Boot leaves it unset
        assertThat(connection.actualValue()).isEqualTo(90_000L); // parsed "90s"
        assertThat(connection.overridden()).isTrue();
        assertThat(connection.note()).isNotBlank();
    }

    @Test
    void reportsConcreteBootDefaultWhenNothingConfigured() {
        var session = setting(PropertyTimeoutDefaultsContributor.servletWeb(new MockEnvironment()), "session-timeout");

        assertThat(session.defaultValue()).isEqualTo(1_800_000L); // 30 minutes
        assertThat(session.actualValue()).isNull();
        assertThat(session.overridden()).isFalse();
    }

    @Test
    void flagsOverrideWhenActualDiffersFromConcreteDefault() {
        var environment = new MockEnvironment().withProperty("server.servlet.session.timeout", "60m");

        var session = setting(PropertyTimeoutDefaultsContributor.servletWeb(environment), "session-timeout");

        assertThat(session.actualValue()).isEqualTo(3_600_000L);
        assertThat(session.overridden()).isTrue();
    }

    // ---- catalogue coverage -------------------------------------------------

    @Test
    void everyCatalogueFactoryProducesANonEmptyMillisecondGroup() {
        var environment = new MockEnvironment();
        List<Function<Environment, PropertyTimeoutDefaultsContributor>> factories = List.of(
                PropertyTimeoutDefaultsContributor::httpClient,
                PropertyTimeoutDefaultsContributor::reactiveHttpClient,
                PropertyTimeoutDefaultsContributor::tomcat,
                PropertyTimeoutDefaultsContributor::jetty,
                PropertyTimeoutDefaultsContributor::reactorNetty,
                PropertyTimeoutDefaultsContributor::servletWeb,
                PropertyTimeoutDefaultsContributor::redis,
                PropertyTimeoutDefaultsContributor::elasticsearch,
                PropertyTimeoutDefaultsContributor::kafka);

        assertThat(factories).allSatisfy(factory -> {
            var group = factory.apply(environment).contribute();
            assertThat(group.category()).isEqualTo("timeouts");
            assertThat(group.dependency()).isNotBlank();
            assertThat(group.settings()).isNotEmpty().allSatisfy(s -> {
                assertThat(s.unit()).isEqualTo("ms");
                // Boot leaves the value unset in tests, so every default is either a concrete
                // number or null-with-note; either way the actual value is unset here.
                assertThat(s.actualValue()).isNull();
                if (s.defaultValue() == null)
                    assertThat(s.note()).isNotBlank();
            });
        });
    }

    @Test
    void elasticsearchAndRedisCarryTheirDocumentedDefaults() {
        var environment = new MockEnvironment();

        var elasticsearch = PropertyTimeoutDefaultsContributor.elasticsearch(environment).contribute();
        assertThat(setting(elasticsearch, "connection-timeout").defaultValue()).isEqualTo(1_000L);
        assertThat(setting(elasticsearch, "socket-timeout").defaultValue()).isEqualTo(30_000L);

        var redis = PropertyTimeoutDefaultsContributor.redis(environment).contribute();
        assertThat(setting(redis, "lettuce-shutdown-timeout").defaultValue()).isEqualTo(100L);
        assertThat(setting(redis, "command-timeout").defaultValue()).isNull();
    }

    private static DefaultSetting setting(PropertyTimeoutDefaultsContributor contributor, String key) {
        return setting(contributor.contribute(), key);
    }

    private static DefaultSetting setting(DefaultsGroup group, String key) {
        return group.settings().stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }
}
