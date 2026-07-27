# actuator-spring-boot-defaults

A small **Spring Boot library** that adds an actuator endpoint exposing, as JSON, the **framework
defaults** and the **values actually in force** for the dependencies your application ships.

The only prerequisites are **Java 25** and **Spring Boot 4.0.7**. The library makes no other
assumption about the host application: support for a dependency activates only when that dependency
is on the classpath, and nothing is forced onto you.

The first release covers **timeouts**.

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
  "groups": [
    {
      "category": "timeouts",
      "dependency": "HikariCP (JDBC connection pool)",
      "settings": [
        {
          "key": "connection-timeout",
          "property": "spring.datasource.hikari.connection-timeout",
          "unit": "ms",
          "defaultValue": 30000,
          "actualValue": 45000,
          "overridden": true,
          "note": null
        }
      ]
    }
  ]
}
```

- `defaultValue` — the framework/library default (read from the library itself, so it tracks the
  version you ship). `null` means the framework leaves it unset and the component's own default applies.
- `actualValue` — the value in force; `null` when nothing is configured (e.g. no data source).
- `overridden` — `true` when `actualValue` is set and differs from `defaultValue`.

## Covered dependencies (first step: timeouts)

| Dependency | Activates when on classpath | Settings |
|------------|-----------------------------|----------|
| HikariCP (JDBC — PostgreSQL, MySQL, …) | `com.zaxxer.hikari.HikariDataSource` | connection-, validation-, idle-, max-lifetime, keepalive |
| MongoDB driver | `com.mongodb.MongoClientSettings` | connect-, read-, server-selection-, max-wait |
| Spring HTTP client (RestClient / RestTemplate) | `org.springframework.web.client.RestClient` | connect-, read-timeout (`spring.http.client.*`) |

A dependency that is **not** on the classpath produces **no group** — nothing is guessed.

## Design

`BootDefaultsEndpoint` (the actuator surface) is a thin adapter over `DefaultsService` (the logic),
which aggregates the `DefaultsContributor` beans present in the context. Each contributor is
registered behind `@ConditionalOnClass`, so adding a new dependency is a new contributor plus a
guarded config block — no change to the service or the endpoint.

## Requirements

- Java 25
- Spring Boot 4.0.7
