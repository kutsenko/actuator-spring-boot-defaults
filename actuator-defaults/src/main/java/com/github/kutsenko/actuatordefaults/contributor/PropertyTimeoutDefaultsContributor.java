package com.github.kutsenko.actuatordefaults.contributor;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.github.kutsenko.actuatordefaults.DefaultsContributor;
import com.github.kutsenko.actuatordefaults.DefaultsGroup;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.env.Environment;

/**
 * A {@link DefaultsContributor} for dependencies whose timeouts are plain Spring Boot configuration
 * properties. It reads the value in force from the {@link Environment} and pairs it with the known
 * Boot default — so it references none of the integration's own types and therefore needs no compile
 * dependency on it. Each supported dependency is a {@link #httpClient(Environment) static factory}
 * carrying that dependency's curated timeout catalogue.
 *
 * <p>Defaults were taken from the Boot 4.0.7 {@code spring-configuration-metadata.json} of each
 * module. Where Boot ships a concrete numeric default it is reported; where Boot leaves the property
 * unset (the component's own default then applies) the default is {@code null} with an explanatory
 * note. Reused across dependencies via {@link Spec#of}/{@link Spec#unset}.
 */
public final class PropertyTimeoutDefaultsContributor implements DefaultsContributor {

    /**
     * One property-backed timeout.
     *
     * @param key           short identifier
     * @param property      the Spring property that configures it
     * @param defaultMillis the Boot default in ms, or {@code null} when Boot leaves it unset
     * @param note          optional clarification (required in practice when {@code defaultMillis} is null)
     */
    public record Spec(String key, String property, Long defaultMillis, String note) {

        public Spec {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(property, "property");
        }

        /** A property with a concrete Boot default. */
        public static Spec of(String key, String property, long defaultMillis) {
            return new Spec(key, property, defaultMillis, null);
        }

        /** A property Boot leaves unset — no numeric default, only a note on what applies instead. */
        public static Spec unset(String key, String property, String note) {
            return new Spec(key, property, null, note);
        }
    }

    private final String dependency;
    private final List<Spec> specs;
    private final Environment environment;

    public PropertyTimeoutDefaultsContributor(String dependency, List<Spec> specs, Environment environment) {
        this.dependency = Objects.requireNonNull(dependency, "dependency");
        this.specs = List.copyOf(specs);
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    @Override
    public DefaultsGroup contribute() {
        var settings = specs.stream().map(this::toSetting).toList();
        return new DefaultsGroup("timeouts", dependency, settings);
    }

    private DefaultSetting toSetting(Spec spec) {
        var actual = millis(environment.getProperty(spec.property()));
        return DefaultSetting.ofMillis(spec.key(), spec.property(), spec.defaultMillis(), actual, spec.note());
    }

    /** Parses a Boot-style duration ({@code "5s"}, {@code "30m"}, {@code "500ms"}, {@code "PT5S"}, {@code "5000"}). */
    private static Long millis(String value) {
        if (value == null || value.isBlank())
            return null;
        return DurationStyle.detectAndParse(value.trim()).toMillis();
    }

    // ---- catalogues: one factory per dependency ---------------------------------------

    private static final String CLIENT_UNSET =
            "Spring Boot leaves this unset; the underlying client's own default applies (usually no timeout).";

    /** Blocking HTTP client — {@code RestClient} / {@code RestTemplate}. */
    public static PropertyTimeoutDefaultsContributor httpClient(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Spring HTTP client (RestClient / RestTemplate)", List.of(
                Spec.unset("connect-timeout", "spring.http.client.connect-timeout", CLIENT_UNSET),
                Spec.unset("read-timeout", "spring.http.client.read-timeout", CLIENT_UNSET)), environment);
    }

    /** Reactive HTTP client — {@code WebClient}. */
    public static PropertyTimeoutDefaultsContributor reactiveHttpClient(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Spring reactive HTTP client (WebClient)", List.of(
                Spec.unset("connect-timeout", "spring.http.reactiveclient.connect-timeout", CLIENT_UNSET),
                Spec.unset("read-timeout", "spring.http.reactiveclient.read-timeout", CLIENT_UNSET)), environment);
    }

    /** Embedded Tomcat (servlet). */
    public static PropertyTimeoutDefaultsContributor tomcat(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Embedded Tomcat (servlet web server)", List.of(
                Spec.unset("connection-timeout", "server.tomcat.connection-timeout",
                        "Spring Boot leaves this unset; Tomcat's own default applies (connectionTimeout 60s)."),
                Spec.unset("keep-alive-timeout", "server.tomcat.keep-alive-timeout",
                        "Spring Boot leaves this unset; Tomcat defaults keep-alive to the connection timeout.")),
                environment);
    }

    /** Embedded Jetty (servlet). */
    public static PropertyTimeoutDefaultsContributor jetty(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Embedded Jetty (servlet web server)", List.of(
                Spec.unset("connection-idle-timeout", "server.jetty.connection-idle-timeout",
                        "Spring Boot leaves this unset; Jetty's own default applies (idle timeout 30s)."),
                Spec.of("threads-idle-timeout", "server.jetty.threads.idle-timeout", 60_000L)), environment);
    }

    /** Embedded Reactor Netty (reactive web server). */
    public static PropertyTimeoutDefaultsContributor reactorNetty(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Embedded Reactor Netty (reactive web server)", List.of(
                Spec.unset("connection-timeout", "server.netty.connection-timeout",
                        "Spring Boot leaves this unset; Reactor Netty's own default applies."),
                Spec.unset("idle-timeout", "server.netty.idle-timeout",
                        "Spring Boot leaves this unset; no idle timeout is applied by default.")), environment);
    }

    /** Servlet web (Spring MVC): HTTP session lifetime and async request timeout. */
    public static PropertyTimeoutDefaultsContributor servletWeb(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Servlet web (Spring MVC)", List.of(
                Spec.of("session-timeout", "server.servlet.session.timeout", 1_800_000L), // 30 minutes
                Spec.unset("async-request-timeout", "spring.mvc.async.request-timeout",
                        "Spring Boot leaves this unset; no async request timeout is applied by default.")),
                environment);
    }

    /** Redis via Spring Data Redis (Lettuce). */
    public static PropertyTimeoutDefaultsContributor redis(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Redis (Spring Data Redis / Lettuce)", List.of(
                Spec.unset("command-timeout", "spring.data.redis.timeout",
                        "Spring Boot leaves this unset; Lettuce's own command timeout applies (60s)."),
                Spec.unset("connect-timeout", "spring.data.redis.connect-timeout",
                        "Spring Boot leaves this unset; Lettuce's own connect timeout applies (10s)."),
                Spec.of("lettuce-shutdown-timeout", "spring.data.redis.lettuce.shutdown-timeout", 100L)), environment);
    }

    /** Elasticsearch low-level REST client. */
    public static PropertyTimeoutDefaultsContributor elasticsearch(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Elasticsearch REST client", List.of(
                Spec.of("connection-timeout", "spring.elasticsearch.connection-timeout", 1_000L),
                Spec.of("socket-timeout", "spring.elasticsearch.socket-timeout", 30_000L)), environment);
    }

    /**
     * Apache Kafka (Spring for Apache Kafka). Spring Boot leaves every timeout unset, delegating to
     * the Kafka client / listener-container defaults noted below. Only properties common to Boot 4.0
     * and 4.1 are listed (the 4.1-only {@code spring.kafka.template.close-timeout} is omitted to keep
     * the catalogue version-neutral).
     */
    public static PropertyTimeoutDefaultsContributor kafka(Environment environment) {
        return new PropertyTimeoutDefaultsContributor("Apache Kafka (Spring for Apache Kafka)", List.of(
                Spec.unset("consumer-fetch-max-wait", "spring.kafka.consumer.fetch-max-wait",
                        "Spring Boot leaves this unset; the Kafka consumer default applies "
                                + "(fetch.max.wait.ms = 500ms)."),
                Spec.unset("consumer-max-poll-interval", "spring.kafka.consumer.max-poll-interval",
                        "Spring Boot leaves this unset; the Kafka consumer default applies "
                                + "(max.poll.interval.ms = 5min)."),
                Spec.unset("consumer-heartbeat-interval", "spring.kafka.consumer.heartbeat-interval",
                        "Spring Boot leaves this unset; the Kafka consumer default applies "
                                + "(heartbeat.interval.ms = 3s)."),
                Spec.unset("listener-poll-timeout", "spring.kafka.listener.poll-timeout",
                        "Spring Boot leaves this unset; the listener container default applies (5s)."),
                Spec.unset("admin-close-timeout", "spring.kafka.admin.close-timeout",
                        "Spring Boot leaves this unset; Spring's KafkaAdmin close timeout applies.")),
                environment);
    }
}
