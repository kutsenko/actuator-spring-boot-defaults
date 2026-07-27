plugins {
    id("org.springframework.boot")
}

val springBootVersion = "4.0.7"

dependencies {
    // Spring Boot BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Test
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// The composition root has no branchable logic worth unit-testing; excluding it keeps
// the 80% line-coverage gate honest (it measures hand-written domain code, not the
// Spring @SpringBootApplication bootstrap).
tasks.jacocoTestReport {
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude("**/ActuatorDefaultsApplication.class") }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude("**/ActuatorDefaultsApplication.class") }
        })
    )
}
