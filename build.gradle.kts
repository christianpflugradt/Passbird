
import groovy.lang.Closure
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    idea
    application
    jacoco
    java
    kotlin("jvm") version "2.2.20"
    id("com.github.jk1.dependency-license-report") version "2.9"
    id("org.owasp.dependencycheck") version "12.1.8"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
    mavenCentral()
}

val guavaVersion = "33.5.0-jre"
val guiceVersion = "7.0.0"
val jacksonVersion = "2.20.0"

val archunitVersion = "1.4.1"
val awaitilityVersion = "4.3.0"
val junitPlatformVersion = "6.0.0"
val mockkVersion = "1.14.6"
val striktVersion = "0.35.1"

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
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("com.tngtech.archunit:archunit:$archunitVersion")
}

java.sourceCompatibility = JavaVersion.VERSION_17
group = "de.pflugradts"

tasks.withType<Jar> {
    archiveBaseName.set("passbird")
    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Build-Date"] = SimpleDateFormat("yyyy-MM-dd").format(Date())
        attributes["Created-By"] = project.findProperty("createdBy") ?: "unspecified"
        attributes["Main-Class"] = "de.pflugradts.passbird.application.MainKt"
        attributes["Implementation-Title"] = "Passbird"
        attributes["Implementation-Version"] = version
        attributes["Implementation-Vendor"] = "Christian Pflugradt"
        attributes["Specification-Title"] = "Secure Offline Password Management CLI"
        attributes["Specification-Version"] = version.toString().substringBefore('.')
        attributes["Specification-Vendor"] = "Independent | Christian Pflugradt"
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

ktlint.version = "1.7.1"

ktlint {
    additionalEditorconfig.set(
        mapOf(
            "ktlint_code_style" to "intellij_idea",
            "ktlint_standard_curly-spacing" to "disabled",
            "ktlint_standard_class-signature" to "disabled",
        ),
    )
    reporters { reporter(HTML) }
}

tasks.test {
    useJUnitPlatform { excludeTags("architecture", "integration", "non-headless") }
}

tasks.register<Test>("integration") {
    useJUnitPlatform { includeTags("integration") }
}

tasks.register<Test>("architecture") {
    useJUnitPlatform { includeTags("architecture") }
}

tasks.register<Test>("allTests") {
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    testLogging { events(FAILED, PASSED, SKIPPED) }
    group = VERIFICATION_GROUP
    var testCount = 0
    afterTest(
        object : Closure<Unit>(this) {
            fun doCall() = testCount++
        },
    )
    doLast { println("\nTotal Tests: $testCount") }
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
    data {
        directory = System.getenv("DEPENDENCY_CHECK_DATA_LOCATION") ?: ".dependency-check"
    }
}

licenseReport {
    allowedLicensesFile = file("$projectDir/allowed-licenses.json")
}

tasks.register("preCommitCheck") {
    group = VERIFICATION_GROUP
    description = "Runs all verification tasks used in the pre-commit hook."

    dependsOn(
        "ktlintCheck",
        "checkLicense",
        "jacocoTestCoverageVerification",
        "allTests",
    )
}
