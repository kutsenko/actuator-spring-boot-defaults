// A plain Spring Boot *library* — NOT a runnable application. It ships an actuator
// endpoint + auto-configuration that any Boot 4.0.7 / Java 25 app can pull in. It
// deliberately does NOT apply the `org.springframework.boot` plugin (no bootJar):
// the artifact is an ordinary library jar consumed by other projects.
plugins {
    `java-library`
    `maven-publish`
}

val springBootVersion = "4.0.7"

dependencies {
    // Align every Spring/Boot version with the consumer's Boot BOM.
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    // --- Hard dependencies: the library's own reason to exist -----------------
    // The auto-configuration engine (@AutoConfiguration / @ConditionalOnClass) and the
    // actuator endpoint machinery (@Endpoint / @ConditionalOnAvailableEndpoint). Every
    // Boot app already has these transitively; declared so versions resolve via the BOM.
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework.boot:spring-boot-actuator-autoconfigure")
    // Jackson annotations to shape the endpoint's JSON (property order + derived override flags).
    // Ubiquitous in any Boot app — the actuator already serialises responses with Jackson.
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    // --- Optional integrations: present ONLY at compile time -------------------
    // Each is referenced solely from a @ConditionalOnClass-guarded contributor. Spring
    // evaluates @ConditionalOnClass from bytecode metadata (no class loading), so when a
    // consumer does NOT ship one of these, the matching contributor is simply never created
    // and no defaults are reported for it. compileOnly keeps them off the consumer classpath.
    // Typed contributors read the *actual* values off live beans, so they compile against the
    // library's types (defaults come from the library itself — HikariConfig(), MongoClientSettings).
    compileOnly("com.zaxxer:HikariCP")             // JDBC connection pool (PostgreSQL, MySQL, …)
    compileOnly("org.mongodb:mongodb-driver-core") // MongoDB driver
    //
    // Property-based contributors read the *actual* values off the Environment and never reference
    // the integration's types, so they need NO compile dependency at all — their @ConditionalOnClass
    // guard names the class as a string. This is what keeps the library able to target any consumer:
    // embedded servers (Tomcat/Jetty/Undertow/Reactor Netty), the HTTP clients, Redis, Elasticsearch,
    // etc. are matched purely by classpath presence.

    // --- Tests: pull the optional integrations in so their contributors are exercised ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.zaxxer:HikariCP")
    testImplementation("org.mongodb:mongodb-driver-core")
    // Web + actuator only for the end-to-end test that boots a server and hits the
    // endpoint over HTTP to prove the JSON contract. NOT part of the library's own deps.
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
}

// Consumed as a normal Maven dependency (artifact: com.github.kutsenko:actuator-defaults).
publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}
