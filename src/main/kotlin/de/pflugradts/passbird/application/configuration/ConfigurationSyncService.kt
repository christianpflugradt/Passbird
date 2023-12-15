package de.pflugradts.passbird.application.configuration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.google.inject.Inject
import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation

class ConfigurationSyncService @Inject constructor(
    @Inject private val updatableConfiguration: UpdatableConfiguration,
    @Inject private val systemOperation: SystemOperation,
) : ConfigurationSync {
    override fun sync(directory: Directory) {
        updatableConfiguration.updateDirectory(directory)
        try {
            YAMLMapper.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .build()
                .writeValue(
                    systemOperation.resolvePath(directory, CONFIGURATION_FILENAME.toFileName()).toFile(),
                    updatableConfiguration,
                )
        } catch (ex: Exception) {
            // FIXME error handling
        }
    }
}
