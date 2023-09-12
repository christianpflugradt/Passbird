import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML

plugins {
    idea
    application
    jacoco
    java
    kotlin("jvm") version "1.9.10"
    id("com.github.hierynomus.license") version "0.16.1"
    id("org.owasp.dependencycheck") version "8.4.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    id("com.adarshr.test-logger") version "3.2.0"
    id("com.github.ben-manes.versions") version "0.47.0"
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("com.google.inject:guice:7.0.0")
    implementation("com.google.inject.extensions:guice-assistedinject:7.0.0")
    implementation("com.google.guava:guava:32.1.2-jre")
    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")

    testImplementation("io.strikt:strikt-core:0.34.1")
    testImplementation("io.strikt:strikt-jvm:0.34.1")
    testImplementation("io.mockk:mockk:1.13.7")

    testCompileOnly("org.projectlombok:lombok:1.18.28")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.28")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("com.tngtech.archunit:archunit:1.1.0")
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
        freeCompilerArgs = listOf(
            "-Xjsr305=strict"
        )
        jvmTarget = "17"
    }
}

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
    Pair("architectureTests", "AT")
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
                minimum = 0.6.toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = 0.6.toBigDecimal()
            }
        }
    }
    mustRunAfter(tasks.jacocoTestReport)
}

dependencyCheck {
    failBuildOnCVSS = 3.0f
    suppressionFile = "owasp-suppressions.xml"
    analyzers.apply {
        assemblyEnabled = false
        centralEnabled = false
        nexusEnabled = false
        ossIndexEnabled = false
        retirejs.enabled = false
    }
}
