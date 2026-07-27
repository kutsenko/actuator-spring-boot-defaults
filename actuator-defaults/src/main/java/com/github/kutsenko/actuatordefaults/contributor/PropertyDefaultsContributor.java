package com.github.kutsenko.actuatordefaults.contributor;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.github.kutsenko.actuatordefaults.DefaultsContributor;
import com.github.kutsenko.actuatordefaults.DefaultsGroup;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

/**
 * A {@link DefaultsContributor} for dependencies whose settings are plain Spring Boot configuration
 * properties. It reads the value in force from the {@link Environment} and pairs it with the known
 * Boot default — so it references none of the integration's own types and therefore needs no compile
 * dependency on it. Each supported dependency is a {@link #httpClient(Environment) static factory}
 * carrying that dependency's curated catalogue.
 *
 * <p>Two flavours are produced by the factories:
 * <ul>
 *   <li><b>timeouts</b> (category {@code "timeouts"}, unit {@code "ms"}) — actual values parsed as
 *       Boot {@code Duration}s;
 *   <li><b>io</b> (category {@code "io"}, unit {@code "bytes"}) — actual values parsed as Boot
 *       {@code DataSize}s; the buffer/size/limit knobs that influence I/O.
 * </ul>
 *
 * <p>Defaults were taken from the Boot {@code spring-configuration-metadata.json} of each module and
 * are the values common to Boot 4.0 and 4.1. Where Boot ships a concrete default it is reported;
 * where Boot leaves the property unset (the component's own default then applies) the default is
 * {@code null} with an explanatory note.
 */
public final class PropertyDefaultsContributor implements DefaultsContributor {

    /**
     * One property-backed setting.
     *
     * @param key          short identifier
     * @param property     the Spring property that configures it
     * @param defaultValue the Boot default (normalised to the contributor's unit), or {@code null}
     *                     when Boot leaves it unset
     * @param note         optional clarification (required in practice when {@code defaultValue} is null)
     */
    public record Spec(String key, String property, Long defaultValue, String note) {

        public Spec {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(property, "property");
        }

        /** A property with a concrete Boot default. */
        public static Spec of(String key, String property, long defaultValue) {
            return new Spec(key, property, defaultValue, null);
        }

        /** A property Boot leaves unset — no numeric default, only a note on what applies instead. */
        public static Spec unset(String key, String property, String note) {
            return new Spec(key, property, null, note);
        }
    }

    /** The category/unit/parser triple shared by every catalogue of the same flavour. */
    private record Kind(String category, String unit, Function<String, Long> parser) {
    }

    private static final Kind TIMEOUTS =
            new Kind("timeouts", "ms", v -> DurationStyle.detectAndParse(v).toMillis());
    private static final Kind IO =
            new Kind("io", "bytes", v -> DataSize.parse(v).toBytes());

    private final Kind kind;
    private final String dependency;
    private final List<Spec> specs;
    private final Environment environment;

    private PropertyDefaultsContributor(Kind kind, String dependency, List<Spec> specs, Environment environment) {
        this.kind = kind;
        this.dependency = Objects.requireNonNull(dependency, "dependency");
        this.specs = List.copyOf(specs);
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    @Override
    public DefaultsGroup contribute() {
        var settings = specs.stream().map(this::toSetting).toList();
        return new DefaultsGroup(kind.category(), dependency, settings);
    }

    private DefaultSetting toSetting(Spec spec) {
        var raw = environment.getProperty(spec.property());
        var actual = (raw == null || raw.isBlank()) ? null : kind.parser().apply(raw.trim());
        return DefaultSetting.of(spec.key(), spec.property(), kind.unit(), spec.defaultValue(), actual, spec.note());
    }

    // ---- constructors per category ----------------------------------------------------

    private static PropertyDefaultsContributor timeouts(String dependency, List<Spec> specs, Environment env) {
        return new PropertyDefaultsContributor(TIMEOUTS, dependency, specs, env);
    }

    private static PropertyDefaultsContributor io(String dependency, List<Spec> specs, Environment env) {
        return new PropertyDefaultsContributor(IO, dependency, specs, env);
    }

    /** Parses a human-readable data size ({@code "1MB"}, {@code "8KB"}, {@code "512B"}) to bytes. */
    private static long bytes(String dataSize) {
        return DataSize.parse(dataSize).toBytes();
    }

    // ============ timeout catalogues ===================================================

    private static final String CLIENT_UNSET =
            "Spring Boot leaves this unset; the underlying client's own default applies (usually no timeout).";

    /** Blocking HTTP client — {@code RestClient} / {@code RestTemplate}. */
    public static PropertyDefaultsContributor httpClient(Environment environment) {
        return timeouts("Spring HTTP client (RestClient / RestTemplate)", List.of(
                Spec.unset("connect-timeout", "spring.http.client.connect-timeout", CLIENT_UNSET),
                Spec.unset("read-timeout", "spring.http.client.read-timeout", CLIENT_UNSET)), environment);
    }

    /** Reactive HTTP client — {@code WebClient}. */
    public static PropertyDefaultsContributor reactiveHttpClient(Environment environment) {
        return timeouts("Spring reactive HTTP client (WebClient)", List.of(
                Spec.unset("connect-timeout", "spring.http.reactiveclient.connect-timeout", CLIENT_UNSET),
                Spec.unset("read-timeout", "spring.http.reactiveclient.read-timeout", CLIENT_UNSET)), environment);
    }

    /** Embedded Tomcat (servlet) timeouts. */
    public static PropertyDefaultsContributor tomcat(Environment environment) {
        return timeouts("Embedded Tomcat (servlet web server)", List.of(
                Spec.unset("connection-timeout", "server.tomcat.connection-timeout",
                        "Spring Boot leaves this unset; Tomcat's own default applies (connectionTimeout 60s)."),
                Spec.unset("keep-alive-timeout", "server.tomcat.keep-alive-timeout",
                        "Spring Boot leaves this unset; Tomcat defaults keep-alive to the connection timeout.")),
                environment);
    }

    /** Embedded Jetty (servlet) timeouts. */
    public static PropertyDefaultsContributor jetty(Environment environment) {
        return timeouts("Embedded Jetty (servlet web server)", List.of(
                Spec.unset("connection-idle-timeout", "server.jetty.connection-idle-timeout",
                        "Spring Boot leaves this unset; Jetty's own default applies (idle timeout 30s)."),
                Spec.of("threads-idle-timeout", "server.jetty.threads.idle-timeout", 60_000L)), environment);
    }

    /** Embedded Reactor Netty (reactive web server) timeouts. */
    public static PropertyDefaultsContributor reactorNetty(Environment environment) {
        return timeouts("Embedded Reactor Netty (reactive web server)", List.of(
                Spec.unset("connection-timeout", "server.netty.connection-timeout",
                        "Spring Boot leaves this unset; Reactor Netty's own default applies."),
                Spec.unset("idle-timeout", "server.netty.idle-timeout",
                        "Spring Boot leaves this unset; no idle timeout is applied by default.")), environment);
    }

    /** Servlet web (Spring MVC): HTTP session lifetime and async request timeout. */
    public static PropertyDefaultsContributor servletWeb(Environment environment) {
        return timeouts("Servlet web (Spring MVC)", List.of(
                Spec.of("session-timeout", "server.servlet.session.timeout", 1_800_000L), // 30 minutes
                Spec.unset("async-request-timeout", "spring.mvc.async.request-timeout",
                        "Spring Boot leaves this unset; no async request timeout is applied by default.")),
                environment);
    }

    /** Redis via Spring Data Redis (Lettuce). */
    public static PropertyDefaultsContributor redis(Environment environment) {
        return timeouts("Redis (Spring Data Redis / Lettuce)", List.of(
                Spec.unset("command-timeout", "spring.data.redis.timeout",
                        "Spring Boot leaves this unset; Lettuce's own command timeout applies (60s)."),
                Spec.unset("connect-timeout", "spring.data.redis.connect-timeout",
                        "Spring Boot leaves this unset; Lettuce's own connect timeout applies (10s)."),
                Spec.of("lettuce-shutdown-timeout", "spring.data.redis.lettuce.shutdown-timeout", 100L)), environment);
    }

    /** Elasticsearch low-level REST client. */
    public static PropertyDefaultsContributor elasticsearch(Environment environment) {
        return timeouts("Elasticsearch REST client", List.of(
                Spec.of("connection-timeout", "spring.elasticsearch.connection-timeout", 1_000L),
                Spec.of("socket-timeout", "spring.elasticsearch.socket-timeout", 30_000L)), environment);
    }

    /** Apache Kafka (Spring for Apache Kafka) — first-class timeouts common to Boot 4.0 and 4.1. */
    public static PropertyDefaultsContributor kafka(Environment environment) {
        return timeouts("Apache Kafka (Spring for Apache Kafka)", List.of(
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

    // ============ io (buffer / size) catalogues ========================================

    /** Servlet multipart (file upload) size limits. */
    public static PropertyDefaultsContributor servletMultipart(Environment environment) {
        return io("Servlet multipart (file uploads)", List.of(
                Spec.of("max-file-size", "spring.servlet.multipart.max-file-size", bytes("1MB")),
                Spec.of("max-request-size", "spring.servlet.multipart.max-request-size", bytes("10MB")),
                Spec.of("file-size-threshold", "spring.servlet.multipart.file-size-threshold", bytes("0B"))),
                environment);
    }

    /** WebFlux multipart (reactive upload) buffer/size limits. */
    public static PropertyDefaultsContributor webfluxMultipart(Environment environment) {
        return io("WebFlux multipart (reactive uploads)", List.of(
                Spec.of("max-in-memory-size", "spring.webflux.multipart.max-in-memory-size", bytes("256KB")),
                Spec.of("max-headers-size", "spring.webflux.multipart.max-headers-size", bytes("10KB"))),
                environment);
    }

    /** Embedded Tomcat request/response size limits. */
    public static PropertyDefaultsContributor tomcatIo(Environment environment) {
        return io("Embedded Tomcat (request/response size limits)", List.of(
                Spec.of("max-http-request-header-size", "server.max-http-request-header-size", bytes("8KB")),
                Spec.of("max-http-response-header-size", "server.tomcat.max-http-response-header-size", bytes("8KB")),
                Spec.of("max-http-form-post-size", "server.tomcat.max-http-form-post-size", bytes("2MB")),
                Spec.of("max-swallow-size", "server.tomcat.max-swallow-size", bytes("2MB"))), environment);
    }

    /** Embedded Jetty request/response size limits. */
    public static PropertyDefaultsContributor jettyIo(Environment environment) {
        return io("Embedded Jetty (request/response size limits)", List.of(
                Spec.of("max-http-form-post-size", "server.jetty.max-http-form-post-size", bytes("200000B")),
                Spec.of("max-http-response-header-size", "server.jetty.max-http-response-header-size", bytes("16KB"))),
                environment);
    }

    /** Embedded Reactor Netty buffer/size limits. */
    public static PropertyDefaultsContributor reactorNettyIo(Environment environment) {
        return io("Embedded Reactor Netty (buffer/size limits)", List.of(
                Spec.of("max-initial-line-length", "server.netty.max-initial-line-length", bytes("4KB")),
                Spec.of("initial-buffer-size", "server.netty.initial-buffer-size", bytes("128B")),
                Spec.of("h2c-max-content-length", "server.netty.h2c-max-content-length", bytes("0B"))), environment);
    }

    /** Logback rolling-file log sizes. */
    public static PropertyDefaultsContributor logback(Environment environment) {
        return io("Logback rolling log files", List.of(
                Spec.of("max-file-size", "logging.logback.rollingpolicy.max-file-size", bytes("10MB")),
                new Spec("total-size-cap", "logging.logback.rollingpolicy.total-size-cap", bytes("0B"),
                        "0 means no cap on the total size of archived log files.")), environment);
    }
}
