package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.process.migration.MigrationAuthenticationService
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Files
import java.nio.file.Path

class PasswordTreeYolkMigrationTest {
    @field:TempDir
    lateinit var homeDirectory: Path

    private val configuration = mockk<Configuration>()
    private val systemOperation = mockk<SystemOperation>(relaxed = true)
    private val migrationAuthenticationService = mockk<MigrationAuthenticationService>()
    private val passwordTreeYolkMigrationService = mockk<PasswordTreeYolkMigrationService>(relaxed = true)

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration, withPasswordTreeLocation = homeDirectory.toString())
    }

    @Test
    fun `should detect older current password tree requiring yolk migration`() {
        val passwordTreeFile = homeDirectory.resolve("passbird.tree")
        Files.write(passwordTreeFile, wrapLegacyCurrentPasswordTree("tree".toByteArray()))

        val actual = PasswordTreeYolkMigrationDetector(configuration, SystemOperation()).detect()

        expectThat(actual.required).isTrue()
        expectThat(actual.pendingMigrations.single().id) isEqualTo "password-tree-yolk"
    }

    @Test
    fun `should not detect yolk migration for missing empty or current password tree`() {
        val detector = PasswordTreeYolkMigrationDetector(configuration, SystemOperation())
        expectThat(detector.detect().required).isFalse()

        val passwordTreeFile = homeDirectory.resolve("passbird.tree")
        Files.write(passwordTreeFile, byteArrayOf())
        expectThat(detector.detect().required).isFalse()

        Files.write(passwordTreeFile, de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope().wrap("tree".toByteArray()))
        expectThat(detector.detect().required).isFalse()
    }

    @Test
    fun `should run yolk migration with authenticated key copy`() {
        val keyShell = shellOf("key")
        every { migrationAuthenticationService.authenticate(any(), any()) } returns success(
            mockk {
                every { keyCopy() } returns keyShell
            },
        )

        val migration = PasswordTreeYolkMigration(
            migrationAuthenticationService = migrationAuthenticationService,
            passwordTreeYolkMigrationService = passwordTreeYolkMigrationService,
            systemOperation = systemOperation,
        )

        migration.run()

        verify(exactly = 1) { passwordTreeYolkMigrationService.migrate(keyShell) }
        expectThat(migration.id) isEqualTo "password-tree-yolk"
        expectThat(migration.order) isEqualTo 3
    }

    @Test
    fun `should exit with failure status when yolk migration authentication fails`() {
        every { migrationAuthenticationService.authenticate(any(), any()) } returns failure(RuntimeException())

        PasswordTreeYolkMigration(
            migrationAuthenticationService = migrationAuthenticationService,
            passwordTreeYolkMigrationService = passwordTreeYolkMigrationService,
            systemOperation = systemOperation,
        ).run()

        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }
}
