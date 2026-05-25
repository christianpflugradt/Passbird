package de.pflugradts.passbird.application.boot.migration

import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.process.migration.AuthenticatedMigrationLocator
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.MigrationRunner
import de.pflugradts.passbird.application.util.SystemOperation
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class PassbirdMigration @Inject constructor(
    private val authenticatedMigrationLocator: AuthenticatedMigrationLocator,
    private val migrationRequest: MigrationRequest,
    private val migrationRunner: MigrationRunner,
    private val systemOperation: SystemOperation,
) : Bootable {
    override fun boot() {
        migrationRunner.run(migrationRequest + authenticatedMigrationLocator.detect())
        systemOperation.exit()
    }
}
