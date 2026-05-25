package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class PasswordTreeFavoritesMigrationDetectorTest {
    private val configuration = mockk<Configuration>()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val systemOperation = SystemOperation()
    private lateinit var passwordTreeDirectory: Path
    private lateinit var passwordTreeFile: Path

    @BeforeEach
    fun setup() {
        passwordTreeDirectory = Files.createTempDirectory("passbird-favorites-detector")
        passwordTreeFile = passwordTreeDirectory.resolve(ReadableConfiguration.PASSWORD_TREE_FILENAME)
        fakeConfiguration(instance = configuration, withPasswordTreeLocation = passwordTreeDirectory.toString())
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(passwordTreeDirectory.toString()).deleteRecursively()).isTrue()
    }

    @Test
    fun `should not require migration when password tree file does not exist`() {
        expectThat(createDetector().detect().required).isFalse()
    }

    @Test
    fun `should not require migration when password tree file is empty`() {
        Files.createFile(passwordTreeFile)

        expectThat(createDetector().detect().required).isFalse()
    }

    @Test
    fun `should not require migration when password tree already uses latest format`() {
        Files.write(passwordTreeFile, passwordTreeEnvelope.wrap("payload".toByteArray()))

        expectThat(createDetector().detect().required).isFalse()
    }

    private fun createDetector() = PasswordTreeFavoritesMigrationDetector(
        configuration = configuration,
        passwordTreeEnvelope = passwordTreeEnvelope,
        systemOperation = systemOperation,
    )
}
