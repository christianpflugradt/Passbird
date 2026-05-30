package de.pflugradts.passbird.application.configuration
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
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
        YAMLMapper.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
            .build()
            .let { mapper ->
                systemOperation.writeToSensitiveFile(
                    systemOperation.resolvePath(directory, CONFIGURATION_FILENAME.toFileName()),
                ) { outputStream ->
                    mapper.writeValue(outputStream, updatableConfiguration)
                }
            }
        Unit
    }.onFailure {
        reportFailure(ConfigurationFailure(it))
    }
}
