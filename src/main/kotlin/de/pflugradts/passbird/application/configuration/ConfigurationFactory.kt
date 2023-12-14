package de.pflugradts.passbird.application.configuration

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.google.inject.Inject
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_SYSTEM_PROPERTY
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation

class ConfigurationFactory @Inject constructor(@Inject private val systemOperation: SystemOperation) {
    fun loadConfiguration() = configurationFromFile() ?: Configuration(template = true)
    private fun configurationFromFile() =
        try {
            YAMLMapper().readValue(
                systemOperation.resolvePath(
                    System.getProperty(CONFIGURATION_SYSTEM_PROPERTY).toDirectory(),
                    CONFIGURATION_FILENAME.toFileName()
                ).toFile(),
                Configuration::class.java,
            )
        } catch (ex: Exception) {
            // FIXME error handling
            null
        }
}
