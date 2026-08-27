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

class PasswordTreeTrashMigrationTest {
    @field:TempDir
    lateinit var homeDirectory: Path

    private val configuration = mockk<Configuration>()
    private val systemOperation = mockk<SystemOperation>(relaxed = true)
    private val migrationAuthenticationService = mockk<MigrationAuthenticationService>()
    private val passwordTreeTrashMigrationService = mockk<PasswordTreeTrashMigrationService>(relaxed = true)

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration, withPasswordTreeLocation = homeDirectory.toString())
    }

    @Test
    fun `should detect legacy trash password tree requiring migration`() {
        val passwordTreeFile = homeDirectory.resolve("passbird.tree")
        Files.write(passwordTreeFile, wrapLegacyTrashPasswordTree("tree".toByteArray()))

        val actual = PasswordTreeTrashMigrationDetector(configuration, SystemOperation()).detect()

        expectThat(actual.required).isTrue()
        expectThat(actual.pendingMigrations.single().id) isEqualTo "password-tree-trash"
    }

    @Test
    fun `should not detect trash migration for missing empty or current password tree`() {
        val detector = PasswordTreeTrashMigrationDetector(configuration, SystemOperation())
        expectThat(detector.detect().required).isFalse()

        val passwordTreeFile = homeDirectory.resolve("passbird.tree")
        Files.write(passwordTreeFile, byteArrayOf())
        expectThat(detector.detect().required).isFalse()

        Files.write(passwordTreeFile, de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope().wrap("tree".toByteArray()))
        expectThat(detector.detect().required).isFalse()
    }

    @Test
    fun `should run trash migration with authenticated key copy`() {
        val keyShell = shellOf("key")
        every { migrationAuthenticationService.authenticate(any(), any()) } returns success(
            mockk {
                every { keyCopy() } returns keyShell
            },
        )

        val migration = PasswordTreeTrashMigration(
            migrationAuthenticationService = migrationAuthenticationService,
            passwordTreeTrashMigrationService = passwordTreeTrashMigrationService,
            systemOperation = systemOperation,
        )

        migration.run()

        verify(exactly = 1) { passwordTreeTrashMigrationService.migrate(keyShell) }
        expectThat(migration.id) isEqualTo "password-tree-trash"
        expectThat(migration.order) isEqualTo 4
    }

    @Test
    fun `should exit with failure status when trash migration authentication fails`() {
        every { migrationAuthenticationService.authenticate(any(), any()) } returns failure(RuntimeException())

        PasswordTreeTrashMigration(
            migrationAuthenticationService = migrationAuthenticationService,
            passwordTreeTrashMigrationService = passwordTreeTrashMigrationService,
            systemOperation = systemOperation,
        ).run()

        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }
}
