package com.github.kutsenko.actuatordefaults.contributor;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.github.kutsenko.actuatordefaults.DefaultsGroup;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class PropertyDefaultsContributorTest {

    // ---- generic behaviour (timeouts) ---------------------------------------

    @Test
    void parsesConfiguredDurationAndFlagsOverrideAgainstUnsetDefault() {
        var environment = new MockEnvironment().withProperty("server.tomcat.connection-timeout", "90s");

        var connection = setting(PropertyDefaultsContributor.tomcat(environment), "connection-timeout");

        assertThat(connection.defaultValue()).isNull();          // Boot leaves it unset
        assertThat(connection.actualValue()).isEqualTo(90_000L); // parsed "90s"
        assertThat(connection.overridden()).isTrue();
        assertThat(connection.note()).isNotBlank();
    }

    @Test
    void reportsConcreteBootDefaultWhenNothingConfigured() {
        var session = setting(PropertyDefaultsContributor.servletWeb(new MockEnvironment()), "session-timeout");

        assertThat(session.defaultValue()).isEqualTo(1_800_000L); // 30 minutes
        assertThat(session.actualValue()).isNull();
        assertThat(session.overridden()).isFalse();
    }

    @Test
    void flagsOverrideWhenActualDiffersFromConcreteDefault() {
        var environment = new MockEnvironment().withProperty("server.servlet.session.timeout", "60m");

        var session = setting(PropertyDefaultsContributor.servletWeb(environment), "session-timeout");

        assertThat(session.actualValue()).isEqualTo(3_600_000L);
        assertThat(session.overridden()).isTrue();
    }

    // ---- generic behaviour (io / data sizes) --------------------------------

    @Test
    void parsesDataSizeAndReportsBytesWithConcreteDefault() {
        var multipart = PropertyDefaultsContributor.servletMultipart(new MockEnvironment()).contribute();

        assertThat(multipart.category()).isEqualTo("io");
        var maxFile = setting(multipart, "max-file-size");
        assertThat(maxFile.unit()).isEqualTo("bytes");
        assertThat(maxFile.defaultValue()).isEqualTo(1_048_576L);  // 1MB
        assertThat(maxFile.actualValue()).isNull();
        assertThat(maxFile.overridden()).isFalse();
    }

    @Test
    void flagsOverrideWhenDataSizeDiffersFromDefault() {
        var environment = new MockEnvironment().withProperty("spring.servlet.multipart.max-file-size", "5MB");

        var maxFile = setting(PropertyDefaultsContributor.servletMultipart(environment), "max-file-size");

        assertThat(maxFile.actualValue()).isEqualTo(5_242_880L); // 5MB
        assertThat(maxFile.overridden()).isTrue();
    }

    @Test
    void ioCatalogueCarriesDocumentedDefaults() {
        var env = new MockEnvironment();

        var tomcatIo = PropertyDefaultsContributor.tomcatIo(env).contribute();
        assertThat(setting(tomcatIo, "max-http-form-post-size").defaultValue()).isEqualTo(2_097_152L); // 2MB
        assertThat(setting(tomcatIo, "max-http-request-header-size").defaultValue()).isEqualTo(8_192L); // 8KB

        var netty = PropertyDefaultsContributor.reactorNettyIo(env).contribute();
        assertThat(setting(netty, "max-initial-line-length").defaultValue()).isEqualTo(4_096L); // 4KB

        var logback = PropertyDefaultsContributor.logback(env).contribute();
        assertThat(setting(logback, "max-file-size").defaultValue()).isEqualTo(10_485_760L); // 10MB
        assertThat(setting(logback, "total-size-cap").note()).isNotBlank();
    }

    // ---- catalogue coverage -------------------------------------------------

    @Test
    void everyTimeoutFactoryProducesANonEmptyMillisecondGroup() {
        assertThat(timeoutFactories()).allSatisfy(factory ->
                assertGroup(factory, "timeouts", "ms"));
    }

    @Test
    void everyIoFactoryProducesANonEmptyByteGroup() {
        assertThat(ioFactories()).allSatisfy(factory ->
                assertGroup(factory, "io", "bytes"));
    }

    private static void assertGroup(
            Function<Environment, PropertyDefaultsContributor> factory, String category, String unit) {
        var group = factory.apply(new MockEnvironment()).contribute();
        assertThat(group.category()).isEqualTo(category);
        assertThat(group.dependency()).isNotBlank();
        assertThat(group.settings()).isNotEmpty().allSatisfy(s -> {
            assertThat(s.unit()).isEqualTo(unit);
            assertThat(s.actualValue()).isNull(); // nothing configured in a blank environment
            if (s.defaultValue() == null)
                assertThat(s.note()).isNotBlank();
        });
    }

    private static List<Function<Environment, PropertyDefaultsContributor>> timeoutFactories() {
        return List.of(
                PropertyDefaultsContributor::httpClient,
                PropertyDefaultsContributor::reactiveHttpClient,
                PropertyDefaultsContributor::tomcat,
                PropertyDefaultsContributor::jetty,
                PropertyDefaultsContributor::reactorNetty,
                PropertyDefaultsContributor::servletWeb,
                PropertyDefaultsContributor::redis,
                PropertyDefaultsContributor::elasticsearch,
                PropertyDefaultsContributor::kafka);
    }

    private static List<Function<Environment, PropertyDefaultsContributor>> ioFactories() {
        return List.of(
                PropertyDefaultsContributor::servletMultipart,
                PropertyDefaultsContributor::webfluxMultipart,
                PropertyDefaultsContributor::tomcatIo,
                PropertyDefaultsContributor::jettyIo,
                PropertyDefaultsContributor::reactorNettyIo,
                PropertyDefaultsContributor::logback);
    }

    private static DefaultSetting setting(PropertyDefaultsContributor contributor, String key) {
        return setting(contributor.contribute(), key);
    }

    private static DefaultSetting setting(DefaultsGroup group, String key) {
        return group.settings().stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }
}
