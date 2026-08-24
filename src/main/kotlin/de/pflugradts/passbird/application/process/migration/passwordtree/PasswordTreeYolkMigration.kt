package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.failure.LoginFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.process.migration.Migration
import de.pflugradts.passbird.application.process.migration.MigrationAuthenticationService
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.PendingMigration
import de.pflugradts.passbird.application.process.migration.PreLaunchMigrationDetector
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation

private const val PASSWORD_TREE_YOLK_MIGRATION_ID = "password-tree-yolk"

class PasswordTreeYolkMigrationDetector constructor(
    private val configuration: ReadableConfiguration,
    private val systemOperation: SystemOperation,
) : PreLaunchMigrationDetector {
    override fun detect() = if (migrationRequired()) {
        MigrationRequest(setOf(PendingMigration(PASSWORD_TREE_YOLK_MIGRATION_ID)))
    } else {
        MigrationRequest.empty()
    }

    private fun migrationRequired() = systemOperation.exists(filePath) &&
        systemOperation.readBytesFromFile(filePath).let { bytes -> bytes.isNotEmpty() && isLegacyCurrentPasswordTree(bytes) }

    private val filePath get() = systemOperation.resolvePath(
        configuration.adapter.passwordTree.location.toDirectory(),
        PASSWORD_TREE_FILENAME.toFileName(),
    )
}

class PasswordTreeYolkMigration constructor(
    private val migrationAuthenticationService: MigrationAuthenticationService,
    private val passwordTreeYolkMigrationService: PasswordTreeYolkMigrationService,
    private val systemOperation: SystemOperation,
) : Migration {
    override val id = PASSWORD_TREE_YOLK_MIGRATION_ID
    override val order = 3

    override fun run() {
        migrationAuthenticationService.authenticate(maxAttempts = 3)
            .onSuccess { migrationCredentials ->
                passwordTreeYolkMigrationService.migrate(migrationCredentials.keyCopy())
            }
            .onFailure {
                reportFailure(LoginFailure(3))
                systemOperation.exit(FAILURE_EXIT_STATUS)
            }
            .getOrNull()
    }
}
