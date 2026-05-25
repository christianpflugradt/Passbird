package de.pflugradts.passbird.application.boot.migration

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.process.migration.AuthenticatedMigrationLocator
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.MigrationRunner
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import jakarta.inject.Inject
import jakarta.inject.Singleton

private const val MIGRATION_PROMPT =
    "Migration required before using Passbird. Migration will run automatically. Make sure you have an up to date backup. " +
        "If something goes wrong, revert to the previous version. Continue Y/n? "
private const val MIGRATION_SUCCESS = "Migration successful. Please start Passbird again"

@Singleton
class PassbirdMigration @Inject constructor(
    private val authenticatedMigrationLocator: AuthenticatedMigrationLocator,
    private val migrationRequest: MigrationRequest,
    private val migrationRunner: MigrationRunner,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val systemOperation: SystemOperation,
) : Bootable {
    override fun boot() {
        if (migrationRequest.required && userInterfaceAdapterPort.receiveYes(outputOf(shellOf(MIGRATION_PROMPT)))) {
            val pendingMigrations = migrationRequest + authenticatedMigrationLocator.detect()
            migrationRunner.run(pendingMigrations)
            userInterfaceAdapterPort.send(outputOf(shellOf(MIGRATION_SUCCESS)))
        }
        systemOperation.exit()
    }
}
