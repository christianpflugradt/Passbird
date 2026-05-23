package de.pflugradts.passbird.application.configuration

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.google.inject.Inject
import de.pflugradts.passbird.application.Global
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.failure.ConfigurationFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation

class ConfigurationFactory @Inject constructor(private val systemOperation: SystemOperation) {
    fun loadConfiguration() = if (!systemOperation.exists(filePath)) {
        Configuration(template = true)
    } else {
        configurationFromFile()
    }

    private fun configurationFromFile() = try {
        filePath.let {
            YAMLMapper().readValue(
                it.toFile(),
                Configuration::class.java,
            )
        }
    } catch (ex: Exception) {
        reportFailure(ConfigurationFailure(ex))
        systemOperation.exit()
        throw ex
    }

    private val filePath get() = systemOperation.resolvePath(
        Global.homeDirectory,
        CONFIGURATION_FILENAME.toFileName(),
    )
}
