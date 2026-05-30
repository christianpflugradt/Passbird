package de.pflugradts.passbird.application.process.migration

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.passbird.application.process.migration.keystore.KeyStoreFormatMigration
import de.pflugradts.passbird.application.process.migration.keystore.KeyStoreFormatMigrationService
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeFavoritesMigration
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeFavoritesMigrationService
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigration
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigrationService
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MigrationFailureExitStatusTest {
    private val migrationAuthenticationService = mockk<MigrationAuthenticationService>()
    private val systemOperation = mockk<SystemOperation>(relaxed = true)

    @BeforeEach
    fun setup() {
        every { migrationAuthenticationService.authenticate(any(), any()) } returns failure(RuntimeException())
    }

    @Test
    fun `should exit with failure status when keystore migration authentication fails`() {
        KeyStoreFormatMigration(
            keyStoreFormatMigrationService = mockk<KeyStoreFormatMigrationService>(relaxed = true),
            migrationAuthenticationService = migrationAuthenticationService,
            systemOperation = systemOperation,
        ).run()

        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should exit with failure status when password tree key derivation migration authentication fails`() {
        PasswordTreeKeyDerivationMigration(
            migrationAuthenticationService = migrationAuthenticationService,
            passwordTreeKeyDerivationMigrationService = mockk<PasswordTreeKeyDerivationMigrationService>(relaxed = true),
            systemOperation = systemOperation,
        ).run()

        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should exit with failure status when password tree favorites migration authentication fails`() {
        PasswordTreeFavoritesMigration(
            migrationAuthenticationService = migrationAuthenticationService,
            passwordTreeFavoritesMigrationService = mockk<PasswordTreeFavoritesMigrationService>(relaxed = true),
            systemOperation = systemOperation,
        ).run()

        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }
}
