package de.pflugradts.passbird.application.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.inject.Inject
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_SYSTEM_PROPERTY
import de.pflugradts.passbird.application.util.SystemOperation

class ConfigurationFactory @Inject constructor(@Inject private val systemOperation: SystemOperation) {
    fun loadConfiguration() = configurationFromFile() ?: Configuration(template = true)
    private fun configurationFromFile() =
        try {
            ObjectMapper(YAMLFactory()).readValue(
                systemOperation.getPath(System.getProperty(CONFIGURATION_SYSTEM_PROPERTY)).resolve(CONFIGURATION_FILENAME).toFile(),
                Configuration::class.java,
            )
        } catch (ex: Exception) {
            // FIXME error handling
            null
        }
}
