
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML

plugins {
    idea
    application
    jacoco
    java
    kotlin("jvm") version "2.0.21"
    id("org.owasp.dependencycheck") version "11.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
    mavenCentral()
}

val guavaVersion = "33.3.1-jre"
val guiceVersion = "7.0.0"
val jacksonVersion = "2.18.0"

val archunitVersion = "1.3.0"
val awaitilityVersion = "4.2.2"
val junitPlatformVersion = "5.11.2"
val mockkVersion = "1.13.13"
val striktVersion = "0.35.0"

dependencies {

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")

    implementation("com.google.inject:guice:$guiceVersion")
    implementation("com.google.inject.extensions:guice-assistedinject:$guiceVersion")
    implementation("com.google.guava:guava:$guavaVersion")

    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("io.strikt:strikt-jvm:$striktVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")

    testImplementation(platform("org.junit:junit-bom:$junitPlatformVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params")

    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("com.tngtech.archunit:archunit:$archunitVersion")
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
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ktlint.version = "1.2.1"

ktlint {
    additionalEditorconfig.set(
        mapOf(
            "ktlint_code_style" to "intellij_idea",
            "ktlint_standard_curly-spacing" to "disabled",
        ),
    )
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
                minimum = 0.85.toBigDecimal()
            }
        }
    }
    mustRunAfter(tasks.withType<Test>())
}

dependencyCheck {
    failBuildOnCVSS = 0.0f
    nvd.apiKey = System.getenv("NVD_API_KEY")
    nvd.delay = 16000
    suppressionFile = "owasp-suppressions.xml"
    analyzers.apply {
        ossIndex.apply {
            username = System.getenv("OSS_INDEX_USERNAME")
            password = System.getenv("OSS_INDEX_PASSWORD")
        }
        centralEnabled = true // Maven Central
        ossIndexEnabled = true // Sonatype OSS Index
        assemblyEnabled = false // .NET
        nexusEnabled = false // Sonatype Nexus Repository
        retirejs.enabled = false // JavaScript
    }
}
