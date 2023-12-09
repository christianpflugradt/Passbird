import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML

plugins {
    idea
    application
    jacoco
    java
    kotlin("jvm") version "1.9.21"
    id("org.owasp.dependencycheck") version "9.0.4"
    id("org.jlleitschuh.gradle.ktlint") version "12.0.2"
    id("com.github.ben-manes.versions") version "0.50.0"
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
    implementation("com.google.inject:guice:7.0.0")
    implementation("com.google.inject.extensions:guice-assistedinject:7.0.0")
    implementation("com.google.guava:guava:32.1.3-jre")

    testImplementation("io.strikt:strikt-core:0.34.1")
    testImplementation("io.strikt:strikt-jvm:0.34.1")
    testImplementation("io.mockk:mockk:1.13.8")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("com.tngtech.archunit:archunit:1.2.1")
}

java.sourceCompatibility = JavaVersion.VERSION_17
group = "de.pflugradts"

tasks.withType<Jar> {
    archiveBaseName.set("pwman3")
    manifest {
        attributes["Main-Class"] = "de.pflugradts.passbird.application.Main"
        attributes["Implementation-Version"] = version
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

ktlint.version = "0.50.0"

ktlint {
    reporters {
        reporter(HTML)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

listOf(
    Pair("unitTests", "Test"),
    Pair("integrationTests", "IT"),
    Pair("architectureTests", "AT"),
).forEach {
    tasks.register<Test>(it.first) {
        group = VERIFICATION_GROUP
        include("**/*${it.second}.class")
    }
}

val testExecutionData: PatternFilterable = fileTree(project.rootDir.path).include("build/jacoco/*.exec")

tasks.jacocoTestReport {
    executionData(testExecutionData)
}

tasks.jacocoTestReport {
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
