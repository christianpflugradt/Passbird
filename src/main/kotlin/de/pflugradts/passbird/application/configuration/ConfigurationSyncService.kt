package de.pflugradts.passbird.application.configuration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.google.inject.Inject
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.util.SystemOperation

class ConfigurationSyncService @Inject constructor(
    @Inject private val updatableConfiguration: UpdatableConfiguration,
    @Inject private val systemOperation: SystemOperation,
) : ConfigurationSync {
    override fun sync(directory: String) {
        updatableConfiguration.updateDirectory(directory)
        try {
            YAMLMapper.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .build()
                .writeValue(systemOperation.resolvePath(directory, CONFIGURATION_FILENAME)!!.toFile(), updatableConfiguration)
        } catch (ex: Exception) {
            // FIXME error handling
        }
    }
}
