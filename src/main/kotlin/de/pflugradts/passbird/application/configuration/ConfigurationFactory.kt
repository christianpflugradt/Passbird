package de.pflugradts.passbird.application.configuration

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.failure.ConfigurationFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import jakarta.inject.Inject

class ConfigurationFactory @Inject constructor(
    private val systemOperation: SystemOperation,
    private val runContext: RunContext,
) {
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
        runContext.homeDirectory,
        CONFIGURATION_FILENAME.toFileName(),
    )
}
