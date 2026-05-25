package de.pflugradts.passbird.application.boot.migration

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.process.migration.AuthenticatedMigrationLocator
import de.pflugradts.passbird.application.process.migration.MigrationAuthenticationService
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.MigrationRunner
import de.pflugradts.passbird.application.process.migration.PendingMigration
import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class PassbirdMigrationTest {

    private val authenticatedMigrationLocator = mockk<AuthenticatedMigrationLocator>()
    private val migrationAuthenticationService = mockk<MigrationAuthenticationService>(relaxed = true)
    private val migrationRunner = mockk<MigrationRunner>(relaxed = true)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val systemOperation = mockk<SystemOperation>(relaxed = true)
    private val migrationRequest = MigrationRequest(setOf(PendingMigration("keystore-format")))
    private val passbirdMigration = PassbirdMigration(
        authenticatedMigrationLocator = authenticatedMigrationLocator,
        migrationAuthenticationService = migrationAuthenticationService,
        migrationRequest = migrationRequest,
        migrationRunner = migrationRunner,
        userInterfaceAdapterPort = userInterfaceAdapterPort,
        systemOperation = systemOperation,
    )

    @Test
    fun `should run requested and authenticated migrations before exit`() {
        // given
        every { userInterfaceAdapterPort.receiveYes(any()) } returns true
        every { authenticatedMigrationLocator.detect() } returns MigrationRequest(
            setOf(
                PendingMigration("keystore-format"),
                PendingMigration("password-tree-format"),
            ),
        )

        // when
        passbirdMigration.boot()

        // then
        verify(exactly = 1) {
            migrationRunner.run(
                MigrationRequest(
                    setOf(
                        PendingMigration("keystore-format"),
                        PendingMigration("password-tree-format"),
                    ),
                ),
            )
        }
        verify(exactly = 1) { userInterfaceAdapterPort.send(any()) }
        verify(exactly = 1) { migrationAuthenticationService.invalidate() }
        verify(exactly = 1) { systemOperation.exit() }
    }

    @Test
    fun `should exit without running migrations when user declines`() {
        // given
        every { userInterfaceAdapterPort.receiveYes(any()) } returns false

        // when
        passbirdMigration.boot()

        // then
        verify(exactly = 0) { authenticatedMigrationLocator.detect() }
        verify(exactly = 0) { migrationRunner.run(any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        verify(exactly = 1) { migrationAuthenticationService.invalidate() }
        verify(exactly = 1) { systemOperation.exit() }
    }
}
