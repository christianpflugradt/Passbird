
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML
import java.text.SimpleDateFormat
import java.util.Date

fun envOrNull(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

plugins {
    idea
    application
    id("dev.detekt") version "2.0.0-alpha.6"
    id("info.solidsoft.pitest") version "1.19.0"
    jacoco
    java
    kotlin("jvm") version "2.4.10"
    id("com.github.jk1.dependency-license-report") version "3.1.4"
    id("org.owasp.dependencycheck") version "13.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.barfuin.gradle.jacocolog") version "4.0.2"
}

repositories {
    mavenCentral()
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/detekt.yml")
    parallel = true
    source.setFrom("src/main/kotlin", "src/test/kotlin")
}

val jacksonVersion = "2.22.2"
val snakeYamlEngineVersion = "3.1.1"

val archunitVersion = "1.5.0"
val junitPlatformVersion = "6.1.3"
val kotestPropertyVersion = "6.2.4"
val mockkVersion = "1.14.11"
val pitestCoreVersion = "1.22.1"
val pitestJunit5Version = "1.2.3"
val striktVersion = "0.35.1"

dependencies {

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("org.snakeyaml:snakeyaml-engine:$snakeYamlEngineVersion")

    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("io.strikt:strikt-jvm:$striktVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")

    testImplementation(platform("org.junit:junit-bom:$junitPlatformVersion"))
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.kotest:kotest-property:$kotestPropertyVersion")

    testImplementation("com.tngtech.archunit:archunit:$archunitVersion")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17
group = "de.pflugradts"
version = (project.findProperty("releaseVersion") as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: "unspecified"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<Jar> {
    archiveBaseName.set("passbird")
    archiveVersion.set("")
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
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xjdk-release=17")
        jvmTarget.set(JvmTarget.JVM_17)
    }
    if (name == "compileTestKotlin") {
        outputs.cacheIf { false }
    }
}

ktlint.version = "1.8.0"

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
    useJUnitPlatform { excludeTags("architecture", "integration", "non-headless", "property") }
}

tasks.register<Test>("property") {
    useJUnitPlatform { includeTags("property") }
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

pitest {
    targetClasses.set(
        setOf(
            "de.pflugradts.passbird.adapter.passwordtree.PasswordTreeFacade",
            "de.pflugradts.passbird.adapter.passwordtree.PasswordTreeReader",
            "de.pflugradts.passbird.adapter.passwordtree.PasswordTreeWriter",
            "de.pflugradts.passbird.adapter.exchange.FilePasswordExchange",
            "de.pflugradts.passbird.application.process.backup.BackupManager",
            "de.pflugradts.passbird.application.security.AesGcmCipher",
            "de.pflugradts.passbird.application.security.CryptoProviderFactory",
            "de.pflugradts.passbird.application.security.KeyStoreAuthenticationService",
        ),
    )
    targetTests.set(
        setOf(
            "de.pflugradts.passbird.adapter.passwordtree.PasswordTreeFacadeTest",
            "de.pflugradts.passbird.adapter.exchange.FilePasswordExchangeTest",
            "de.pflugradts.passbird.adapter.exchange.FilePasswordExchangeIntegrationTest",
            "de.pflugradts.passbird.application.process.backup.BackupManagerTest",
            "de.pflugradts.passbird.application.security.AesGcmCipherTest",
            "de.pflugradts.passbird.application.security.CryptoProviderFactoryTest",
            "de.pflugradts.passbird.application.security.KeyStoreAuthenticationServiceTest",
        ),
    )
    pitestVersion.set(pitestCoreVersion)
    junit5PluginVersion.set(pitestJunit5Version)
    threads.set(1)
    verbose.set(true)
    timeoutFactor.set("2.0".toBigDecimal())
    timeoutConstInMillis.set(10000)
    outputFormats.set(setOf("XML", "HTML"))
    exportLineCoverage.set(true)
    failWhenNoMutations.set(true)
    timestampedReports.set(false)
}

tasks.register("mutation") {
    group = VERIFICATION_GROUP
    description = "Runs mutation testing for high-risk persistence, exchange, backup, and security code."
    dependsOn("pitest")
}

tasks.withType<Test>().configureEach {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    testLogging { events(FAILED, PASSED, SKIPPED) }
    group = VERIFICATION_GROUP
    var testCount = 0
    addTestListener(
        object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) = Unit

            override fun afterSuite(suite: TestDescriptor, result: TestResult) = Unit

            override fun beforeTest(testDescriptor: TestDescriptor) = Unit

            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
                testCount++
            }
        },
    )
    doLast { println("\nTotal Tests: $testCount") }
}

val testExecutionData: PatternFilterable = fileTree(project.rootDir.path).include("build/jacoco/*.exec")

tasks.jacocoTestReport {
    executionData(testExecutionData)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
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
    scanConfigurations = listOf("runtimeClasspath")
    failBuildOnCVSS = 0.0f
    nvd.apiKey = envOrNull("NVD_API_KEY")
    envOrNull("DEPENDENCY_CHECK_NVD_DATAFEED_URL")?.let { nvd.datafeedUrl = it }
    suppressionFile = "owasp-suppressions.xml"
    envOrNull("DEPENDENCY_CHECK_HOSTED_SUPPRESSIONS_URL")?.let { hostedSuppressions.url = it }
    analyzers.apply {
        envOrNull("DEPENDENCY_CHECK_KEV_URL")?.let { kev.url = it }
        ossIndex.apply {
            username = envOrNull("OSS_INDEX_USERNAME")
            password = envOrNull("OSS_INDEX_PASSWORD")
        }
        nexus {
            enabled.set(false)
        }
        centralEnabled = true // Maven Central
        assemblyEnabled = false // .NET
        retirejs.enabled = false // JavaScript
    }
    data {
        directory = System.getenv("DEPENDENCY_CHECK_DATA_LOCATION") ?: ".dependency-check"
    }
}

licenseReport {
    allowedLicensesFile = file("$projectDir/allowed-licenses.json")
}

val cleanPrePushSmokeDirectory = tasks.register<Delete>("cleanPrePushSmokeDirectory") {
    delete(layout.buildDirectory.dir("smoke-test-pre-push"))
}

val prePushSmokeTest = tasks.register<Exec>("prePushSmokeTest") {
    group = VERIFICATION_GROUP
    description = "Builds the local jar and verifies the interactive smoke flow before push."

    dependsOn(tasks.jar, cleanPrePushSmokeDirectory)
    mustRunAfter("allTests", "jacocoTestCoverageVerification", "checkLicense")
    environment("PASSBIRD_SMOKE_TMPDIR", layout.buildDirectory.dir("smoke-test-pre-push").get().asFile.absolutePath)
    commandLine("./smoke-test/run.sh")
}

tasks.register("preCommitCheck") {
    group = VERIFICATION_GROUP
    description = "Runs the fast local verification tasks used in the pre-commit hook."

    dependsOn(
        "ktlintCheck",
        "detekt",
        "compileKotlin",
        "compileTestKotlin",
    )
}

tasks.register("prePushCheck") {
    group = VERIFICATION_GROUP
    description = "Runs the full local verification tasks used in the pre-push hook."

    dependsOn(
        "checkLicense",
        "jacocoTestCoverageVerification",
        "allTests",
        prePushSmokeTest,
    )
}
