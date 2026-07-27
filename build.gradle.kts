plugins {
    java
    jacoco
    checkstyle
    id("com.github.spotbugs") version "6.5.9" apply false
    id("org.springframework.boot") version "4.0.7" apply false
    id("org.owasp.dependencycheck") version "12.2.2" apply false
    id("org.cyclonedx.bom") version "3.2.4" apply false
}

allprojects {
    group = "com.github.kutsenko"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    // The custom-Checkstyle-rules module is a plain Java library on the Checkstyle
    // tool classpath; it must NOT inherit the application machinery (Boot, JaCoCo
    // 95% coverage, SpotBugs, OWASP, dependency-locking). It configures itself.
    if (name == "checkstyle-rules") return@subprojects

    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "checkstyle")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "org.owasp.dependencycheck")
    apply(plugin = "org.cyclonedx.bom")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("--enable-preview")
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
        // Gradle defaults the Test JVM to 512m, which is too tight for
        // this suite's @SpringBootTest contexts + Testcontainers clients
        // and produced flaky "Java heap space" failures. 2g leaves
        // ample headroom on the CI host.
        maxHeapSize = "2g"
    }

    tasks.withType<JavaExec> {
        jvmArgs("--enable-preview")
    }

    jacoco {
        toolVersion = "0.8.14"
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required = true
            html.required = true
        }
    }

    tasks.jacocoTestCoverageVerification {
        dependsOn(tasks.jacocoTestReport)
        violationRules {
            rule {
                limit {
                    minimum = "0.95".toBigDecimal()
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn(tasks.jacocoTestCoverageVerification)
    }

    checkstyle {
        toolVersion = "13.8.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        configDirectory = rootProject.file("config/checkstyle")
        isIgnoreFailures = false
    }

    // The custom SingleStatementBracesCheck lives in :checkstyle-rules; its JAR is on the
    // Checkstyle tool classpath as a file (see dependencies{}), so build it first.
    tasks.withType<Checkstyle>().configureEach {
        dependsOn(":checkstyle-rules:jar")
    }

    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        toolVersion = "4.9.8"
        effort = com.github.spotbugs.snom.Effort.MAX
        reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
        excludeFilter = rootProject.file("config/spotbugs/exclude-filter.xml")
    }

    tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
        reports.create("html") { required = true }
        reports.create("xml") { required = true }
    }

    configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
        failBuildOnCVSS = 7.0f
        formats = listOf("HTML", "JSON")
        suppressionFile = rootProject.file("config/owasp/suppressions.xml").path
        nvd {
            apiKey = System.getenv("NVD_API_KEY") ?: ""
        }
    }

    dependencies {
        // --- Custom braceless check on the Checkstyle tool classpath (arc42 §8.10.2) ---
        // config/checkstyle/checkstyle.xml references com.github.kutsenko.checkstyle.
        // SingleStatementBracesCheck, which lives in the :checkstyle-rules module.
        //
        // Two Gradle subtleties dictate the exact shape below:
        //  1) The Checkstyle plugin adds `com.puppycrawl.tools:checkstyle:$toolVersion` to this
        //     configuration via `defaultDependencies`, which fire ONLY when nothing else is
        //     declared here. Declaring our check jar suppresses them → the tool jar itself
        //     vanishes (`ClassNotFoundException: CheckstyleAntTask`). So we re-add checkstyle
        //     explicitly (its transitives are already lock-pinned in actuator-defaults/gradle.lockfile).
        //  2) The check is attached as the built JAR *file*, deliberately NOT
        //     `project(":checkstyle-rules")`. A project() dependency stamps JVM-ecosystem variant
        //     attributes onto this legacy attribute-free tool configuration, and Gradle then can't
        //     select a variant for the plain tool jars. A file() carries no attributes and, being
        //     a local file rather than a module, adds nothing to the lockfile.
        "checkstyle"("com.puppycrawl.tools:checkstyle:13.8.0")
        "checkstyle"(files(rootProject.layout.projectDirectory.file(
            "checkstyle-rules/build/libs/checkstyle-rules-$version.jar")))

        testImplementation(platform("org.junit:junit-bom:6.1.2"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.assertj:assertj-core:3.27.7")
        testImplementation("org.mockito:mockito-core:5.23.0")
        testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}
