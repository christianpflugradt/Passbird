package de.pflugradts.passbird.application.process.migration.keystore
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.process.migration.MigrationCredentials
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
class KeyStoreFormatMigrationService constructor(
    private val configuration: ReadableConfiguration,
    private val keyStoreAdapterPort: KeyStoreAdapterPort,
    private val systemOperation: SystemOperation,
) {
    fun migrate(migrationCredentials: MigrationCredentials) {
        keyStoreAdapterPort.storeExistingKey(
            migrationCredentials.keyCopy(),
            migrationCredentials.passwordCopy(),
            filePath,
        )
    }
    private val filePath get() = systemOperation.resolvePath(
        configuration.adapter.keyStore.location.toDirectory(),
        KEYSTORE_FILENAME.toFileName(),
    )
}
