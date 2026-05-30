package de.pflugradts.passbird.application.process.migration.passwordtree
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.failure.LoginFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.process.migration.Migration
import de.pflugradts.passbird.application.process.migration.MigrationAuthenticationService
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.PendingMigration
import de.pflugradts.passbird.application.process.migration.PreLaunchMigrationDetector
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
private const val PASSWORD_TREE_KEY_DERIVATION_MIGRATION_ID = "password-tree-key-derivation"
class PasswordTreeKeyDerivationMigrationDetector constructor(
    private val configuration: ReadableConfiguration,
    private val passwordTreeEnvelope: PasswordTreeEnvelope,
    private val systemOperation: SystemOperation,
) : PreLaunchMigrationDetector {
    override fun detect() = if (migrationRequired()) {
        MigrationRequest(setOf(PendingMigration(PASSWORD_TREE_KEY_DERIVATION_MIGRATION_ID)))
    } else {
        MigrationRequest.empty()
    }
    private fun migrationRequired() = systemOperation.exists(filePath) &&
        systemOperation.readBytesFromFile(filePath).let { bytes ->
            bytes.isNotEmpty() && !passwordTreeEnvelope.isCurrent(bytes) && !passwordTreeEnvelope.isLegacyCurrent(bytes)
        }
    private val filePath get() = systemOperation.resolvePath(
        configuration.adapter.passwordTree.location.toDirectory(),
        PASSWORD_TREE_FILENAME.toFileName(),
    )
}
class PasswordTreeKeyDerivationMigration constructor(
    private val migrationAuthenticationService: MigrationAuthenticationService,
    private val passwordTreeKeyDerivationMigrationService: PasswordTreeKeyDerivationMigrationService,
    private val systemOperation: SystemOperation,
) : Migration {
    override val id = PASSWORD_TREE_KEY_DERIVATION_MIGRATION_ID
    override val order = 2
    override fun run() {
        migrationAuthenticationService.authenticate(maxAttempts = 3)
            .onSuccess { migrationCredentials ->
                passwordTreeKeyDerivationMigrationService.migrate(migrationCredentials.keyCopy())
            }
            .onFailure {
                reportFailure(LoginFailure(3))
                systemOperation.exit(FAILURE_EXIT_STATUS)
            }
            .getOrNull()
    }
}
