package com.github.kutsenko.actuatordefaults.contributor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

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

        assertThat(connection.defaultValue(), is(nullValue()));   // Boot leaves it unset
        assertThat(connection.actualValue(), is(90_000L));        // parsed "90s"
        assertThat(connection.overridden(), is(true));
        assertThat(connection.note(), is(not(blankOrNullString())));
    }

    @Test
    void reportsConcreteBootDefaultWhenNothingConfigured() {
        var session = setting(PropertyDefaultsContributor.servletWeb(new MockEnvironment()), "session-timeout");

        assertThat(session.defaultValue(), is(1_800_000L)); // 30 minutes
        assertThat(session.actualValue(), is(nullValue()));
        assertThat(session.overridden(), is(false));
    }

    @Test
    void flagsOverrideWhenActualDiffersFromConcreteDefault() {
        var environment = new MockEnvironment().withProperty("server.servlet.session.timeout", "60m");

        var session = setting(PropertyDefaultsContributor.servletWeb(environment), "session-timeout");

        assertThat(session.actualValue(), is(3_600_000L));
        assertThat(session.overridden(), is(true));
    }

    // ---- generic behaviour (io / data sizes) --------------------------------

    @Test
    void parsesDataSizeAndReportsBytesWithConcreteDefault() {
        var multipart = PropertyDefaultsContributor.servletMultipart(new MockEnvironment()).contribute();

        assertThat(multipart.category(), is("io"));
        var maxFile = setting(multipart, "max-file-size");
        assertThat(maxFile.unit(), is("bytes"));
        assertThat(maxFile.defaultValue(), is(1_048_576L));  // 1MB
        assertThat(maxFile.actualValue(), is(nullValue()));
        assertThat(maxFile.overridden(), is(false));
    }

    @Test
    void flagsOverrideWhenDataSizeDiffersFromDefault() {
        var environment = new MockEnvironment().withProperty("spring.servlet.multipart.max-file-size", "5MB");

        var maxFile = setting(PropertyDefaultsContributor.servletMultipart(environment), "max-file-size");

        assertThat(maxFile.actualValue(), is(5_242_880L)); // 5MB
        assertThat(maxFile.overridden(), is(true));
    }

    @Test
    void ioCatalogueCarriesDocumentedDefaults() {
        var env = new MockEnvironment();

        var tomcatIo = PropertyDefaultsContributor.tomcatIo(env).contribute();
        assertThat(setting(tomcatIo, "max-http-form-post-size").defaultValue(), is(2_097_152L)); // 2MB
        assertThat(setting(tomcatIo, "max-http-request-header-size").defaultValue(), is(8_192L)); // 8KB

        var netty = PropertyDefaultsContributor.reactorNettyIo(env).contribute();
        assertThat(setting(netty, "max-initial-line-length").defaultValue(), is(4_096L)); // 4KB

        var logback = PropertyDefaultsContributor.logback(env).contribute();
        assertThat(setting(logback, "max-file-size").defaultValue(), is(10_485_760L)); // 10MB
        assertThat(setting(logback, "total-size-cap").note(), is(not(blankOrNullString())));
    }

    // ---- catalogue coverage -------------------------------------------------

    @Test
    void everyTimeoutFactoryProducesANonEmptyMillisecondGroup() {
        for (var factory : timeoutFactories())
            assertGroup(factory, "timeouts", "ms");
    }

    @Test
    void everyIoFactoryProducesANonEmptyByteGroup() {
        for (var factory : ioFactories())
            assertGroup(factory, "io", "bytes");
    }

    private static void assertGroup(
            Function<Environment, PropertyDefaultsContributor> factory, String category, String unit) {
        var group = factory.apply(new MockEnvironment()).contribute();
        assertThat(group.category(), is(category));
        assertThat(group.dependency(), is(not(blankOrNullString())));
        assertThat(group.settings(), is(not(empty())));
        for (var s : group.settings()) {
            assertThat(s.unit(), is(unit));
            assertThat(s.actualValue(), is(nullValue())); // nothing configured in a blank environment
            if (s.defaultValue() == null)
                assertThat(s.note(), is(not(blankOrNullString())));
        }
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
