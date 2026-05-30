package de.pflugradts.passbird.application.process.migration.keystore

import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.failure.LoginFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.keystore.KeyStoreFormat
import de.pflugradts.passbird.application.keystore.KeyStoreFormatDetector
import de.pflugradts.passbird.application.process.migration.Migration
import de.pflugradts.passbird.application.process.migration.MigrationAuthenticationService
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.PendingMigration
import de.pflugradts.passbird.application.process.migration.PreLaunchMigrationDetector
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
import jakarta.inject.Inject
import jakarta.inject.Singleton

private const val KEYSTORE_FORMAT_MIGRATION_ID = "keystore-format"

@Singleton
class KeyStoreFormatMigrationDetector @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val keyStoreFormatDetector: KeyStoreFormatDetector,
    private val systemOperation: SystemOperation,
) : PreLaunchMigrationDetector {
    override fun detect() = if (migrationRequired()) {
        MigrationRequest(setOf(PendingMigration(KEYSTORE_FORMAT_MIGRATION_ID)))
    } else {
        MigrationRequest.empty()
    }

    private fun migrationRequired() = systemOperation.exists(filePath) &&
        keyStoreFormatDetector.detect(systemOperation.readBytesFromFile(filePath)) == KeyStoreFormat.JCEKS

    private val filePath get() = systemOperation.resolvePath(
        configuration.adapter.keyStore.location.toDirectory(),
        KEYSTORE_FILENAME.toFileName(),
    )
}

@Singleton
class KeyStoreFormatMigration @Inject constructor(
    private val keyStoreFormatMigrationService: KeyStoreFormatMigrationService,
    private val migrationAuthenticationService: MigrationAuthenticationService,
    private val systemOperation: SystemOperation,
) : Migration {
    override val id = KEYSTORE_FORMAT_MIGRATION_ID
    override val order = 1

    override fun run() {
        migrationAuthenticationService.authenticate(maxAttempts = 3)
            .onSuccess(keyStoreFormatMigrationService::migrate)
            .onFailure {
                reportFailure(LoginFailure(3))
                systemOperation.exit(FAILURE_EXIT_STATUS)
            }
            .getOrNull()
    }
}
