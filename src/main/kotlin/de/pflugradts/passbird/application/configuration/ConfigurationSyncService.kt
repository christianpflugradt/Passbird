package de.pflugradts.passbird.application.configuration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
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
            ObjectMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER) // FIXME deprecated
                .writeValue(systemOperation.resolvePath(directory, CONFIGURATION_FILENAME)!!.toFile(), updatableConfiguration)
        } catch (ex: Exception) {
            // FIXME error handling
        }
    }
}
