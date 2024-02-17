import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML

plugins {
    idea
    application
    jacoco
    java
    kotlin("jvm") version "1.9.22"
    id("org.owasp.dependencycheck") version "9.0.9"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1")
    implementation("com.google.inject:guice:7.0.0")
    implementation("com.google.inject.extensions:guice-assistedinject:7.0.0")
    implementation("com.google.guava:guava:33.0.0-jre")

    testImplementation("io.strikt:strikt-core:0.34.1")
    testImplementation("io.strikt:strikt-jvm:0.34.1")
    testImplementation("io.mockk:mockk:1.13.9")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("com.tngtech.archunit:archunit:1.2.1")
}

java.sourceCompatibility = JavaVersion.VERSION_17
group = "de.pflugradts"

tasks.withType<Jar> {
    archiveBaseName.set("passbird")
    manifest {
        attributes["Main-Class"] = "de.pflugradts.passbird.application.MainKt"
        attributes["Implementation-Version"] = version
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(if (version != "codeql") sourceSets.main.get().output else sourceSets.main.get().output + sourceSets.test.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

ktlint.version = "1.1.1"

ktlint {
    reporters {
        reporter(HTML)
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("architecture", "integration", "non-headless")
    }
}

tasks.register<Test>("integration") {
    useJUnitPlatform {
        includeTags("integration")
    }
}

tasks.register<Test>("architecture") {
    useJUnitPlatform {
        includeTags("architecture")
    }
}

tasks.register<Test>("all") {
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    testLogging {
        events(FAILED, PASSED, SKIPPED)
    }
    group = VERIFICATION_GROUP
}

val testExecutionData: PatternFilterable = fileTree(project.rootDir.path).include("build/jacoco/*.exec")

tasks.jacocoTestReport {
    executionData(testExecutionData)
    mustRunAfter(tasks.withType<Test>())
    mustRunAfter(tasks.ktlintKotlinScriptCheck)
    mustRunAfter(tasks.ktlintMainSourceSetCheck)
    mustRunAfter(tasks.ktlintTestSourceSetCheck)
    mustRunAfter(tasks.startScripts)
}

tasks.jacocoTestCoverageVerification {
    executionData(testExecutionData)
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                minimum = 0.9.toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = 0.9.toBigDecimal()
            }
        }
    }
    mustRunAfter(tasks.withType<Test>())
    mustRunAfter(tasks.jacocoTestReport)
}

dependencyCheck {
    failBuildOnCVSS = 0.0f
    nvd.apiKey = System.getenv("NVD_API_KEY")
    nvd.delay = 16000
    suppressionFile = "owasp-suppressions.xml"
    analyzers.apply {
        assemblyEnabled = false
        centralEnabled = false
        nexusEnabled = false
        ossIndexEnabled = false
        retirejs.enabled = false
    }
}
