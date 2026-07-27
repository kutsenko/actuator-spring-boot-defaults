# actuator-spring-boot-defaults

A small **Spring Boot library** that adds an actuator endpoint exposing, as JSON, the **framework
defaults** and the **values actually in force** for the dependencies your application ships.

The only prerequisites are **Java 25** and **Spring Boot 4.1.0**. The library makes no other
assumption about the host application: support for a dependency activates only when that dependency
is on the classpath, and nothing is forced onto you.

The first releases cover **timeouts** (category `timeouts`, unit `ms`) and **I/O size limits**
(category `io`, unit `bytes`) — buffers, file-upload and request/response size knobs.

## Install

The library is a plain `java-library` jar (no `bootJar`). Publish it locally and depend on it:

```bash
./gradlew :actuator-defaults:publishToMavenLocal
```

```kotlin
dependencies {
    implementation("com.github.kutsenko:actuator-defaults:0.1.0-SNAPSHOT")
}
```

Expose the endpoint (it is disabled by default, like all non-standard actuator endpoints):

```properties
management.endpoints.web.exposure.include=bootdefaults
```

## Use

```
GET /actuator/bootdefaults
```

```json
{
  "hasOverrides": true,
  "overriddenCount": 1,
  "groups": [
    {
      "dependency": "HikariCP (JDBC connection pool)",
      "category": "timeouts",
      "hasOverrides": true,
      "overriddenCount": 1,
      "settings": [
        {
          "key": "connection-timeout",
          "property": "spring.datasource.hikari.connection-timeout",
          "overridden": true,
          "defaultValue": 30000,
          "actualValue": 45000,
          "unit": "ms",
          "note": null
        }
      ]
    }
  ]
}
```

`hasOverrides` / `overriddenCount` are repeated at the report and group level so the caller sees at a
glance — without comparing every value — whether and where the application diverges from the defaults.

- `defaultValue` — the framework/library default (read from the library itself, so it tracks the
  version you ship). `null` means the framework leaves it unset and the component's own default applies.
- `actualValue` — the value in force; `null` when nothing is configured (e.g. no data source).
- `overridden` — `true` when `actualValue` is set and differs from `defaultValue`.

**Ordering:** overridden values surface first. Groups are ordered by how many settings they override
(most first), and within each group the overridden settings come before the untouched ones — so what
you actually changed sits at the top of the response.

## Covered dependencies

### Timeouts (`category: timeouts`, `unit: ms`)

| Dependency | Activates when on classpath (guard) | Settings |
|------------|-------------------------------------|----------|
| HikariCP (JDBC — PostgreSQL, MySQL, …) | `com.zaxxer.hikari.HikariDataSource` | connection-, validation-, idle-, max-lifetime, keepalive |
| MongoDB driver | `com.mongodb.MongoClientSettings` | connect-, read-, server-selection-, max-wait |
| Spring HTTP client (RestClient / RestTemplate) | `org.springframework.web.client.RestClient` | connect-, read-timeout (`spring.http.client.*`) |
| Spring reactive HTTP client (WebClient) | `…reactive.function.client.WebClient` | connect-, read-timeout (`spring.http.reactiveclient.*`) |
| Embedded Tomcat | `org.apache.catalina.startup.Tomcat` | `server.tomcat.connection-timeout`, `keep-alive-timeout` |
| Embedded Jetty | `org.eclipse.jetty.server.Server` | `server.jetty.connection-idle-timeout`, `threads.idle-timeout` |
| Embedded Reactor Netty (reactive) | `reactor.netty.http.server.HttpServer` | `server.netty.connection-timeout`, `idle-timeout` |
| Servlet web (Spring MVC) | `…web.servlet.DispatcherServlet` | `server.servlet.session.timeout`, `spring.mvc.async.request-timeout` |
| Redis (Spring Data Redis / Lettuce) | `…redis.connection.RedisConnectionFactory` | `spring.data.redis.timeout`, `connect-timeout`, `lettuce.shutdown-timeout` |
| Elasticsearch REST client | `org.elasticsearch.client.RestClient` | `spring.elasticsearch.connection-timeout`, `socket-timeout` |
| Apache Kafka (Spring for Apache Kafka) | `org.springframework.kafka.core.KafkaTemplate` | `spring.kafka.consumer.*`, `listener.poll-timeout`, `admin.close-timeout` |

### I/O sizes (`category: io`, `unit: bytes`)

| Dependency | Activates when on classpath (guard) | Settings |
|------------|-------------------------------------|----------|
| Servlet multipart (file uploads) | `…web.servlet.DispatcherServlet` | `spring.servlet.multipart.max-file-size`, `max-request-size`, `file-size-threshold` |
| WebFlux multipart (reactive uploads) | `…reactive.function.client.WebClient` | `spring.webflux.multipart.max-in-memory-size`, `max-headers-size` |
| Embedded Tomcat | `org.apache.catalina.startup.Tomcat` | `server.max-http-request-header-size`, `server.tomcat.max-http-response-header-size`, `max-http-form-post-size`, `max-swallow-size` |
| Embedded Jetty | `org.eclipse.jetty.server.Server` | `server.jetty.max-http-form-post-size`, `max-http-response-header-size` |
| Embedded Reactor Netty | `reactor.netty.http.server.HttpServer` | `server.netty.max-initial-line-length`, `initial-buffer-size`, `h2c-max-content-length` |
| Logback rolling log files | `ch.qos.logback.classic.LoggerContext` | `logging.logback.rollingpolicy.max-file-size`, `total-size-cap` |

A dependency that is **not** on the classpath produces **no group** — nothing is guessed. (Undertow is
not covered: Spring Boot 4.0 no longer ships it as an embedded server.)

**Analysed but not covered — MinIO and Keycloak.** Neither exposes its timeouts through Spring Boot
configuration properties: the MinIO SDK sets connect/write/read timeouts programmatically on its OkHttp
client (`MinioClient.setTimeout(...)`, no getter), and the Keycloak admin client configures them on the
underlying JAX-RS/Resteasy client. With no property surface and no readable effective value, a
contributor could show neither a meaningful actual value nor the `overridden` flag, so they are left out
rather than reported with always-null values.

## Design

`BootDefaultsEndpoint` (the actuator surface) is a thin adapter over `DefaultsService` (the logic),
which aggregates the `DefaultsContributor` beans present in the context. Contributors come in two
flavours, both registered behind a `@ConditionalOnClass` guard:

- **Typed** (Hikari, Mongo): read the actual values off the live bean and the defaults from the
  library itself (`new HikariConfig()`, `MongoClientSettings.builder().build()`).
- **Property-based** (`PropertyTimeoutDefaultsContributor`): read the actual values off the
  `Environment` and pair them with the Boot default from the module's configuration metadata. These
  reference none of the integration's types, so the guard names the class as a string and the library
  needs **no compile dependency** on it. Adding a dependency is one catalogue factory + one guarded block.

## Requirements

- Java 25
- Spring Boot 4.1.0
