package de.pflugradts.passbird.application.configuration
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.failure.ConfigurationFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
class ConfigurationSyncService constructor(
    private val updatableConfiguration: UpdatableConfiguration,
    private val systemOperation: SystemOperation,
    private val configurationYamlMapper: ConfigurationYamlMapper = ConfigurationYamlMapper(),
) : ConfigurationSync {
    override fun sync(directory: Directory): TryResult<Unit> = writeConfiguration(directory) {
        updatableConfiguration.updateDirectory(directory)
    }

    override fun syncKeyStoreLocation(configurationDirectory: Directory, keyStoreDirectory: Directory): TryResult<Unit> =
        writeConfiguration(configurationDirectory) {
            updatableConfiguration.updateKeyStoreDirectory(keyStoreDirectory)
        }

    private fun writeConfiguration(directory: Directory, updateConfiguration: () -> Unit): TryResult<Unit> = tryCatching {
        updateConfiguration()
        val yaml = configurationYamlMapper.writeConfiguration(updatableConfiguration)
        systemOperation.writeToSensitiveFile(
            systemOperation.resolvePath(directory, CONFIGURATION_FILENAME.toFileName()),
        ) { outputStream ->
            outputStream.writer(Charsets.UTF_8).use { writer ->
                writer.write(yaml)
            }
        }
        Unit
    }.onFailure {
        reportFailure(ConfigurationFailure(it))
    }
}
