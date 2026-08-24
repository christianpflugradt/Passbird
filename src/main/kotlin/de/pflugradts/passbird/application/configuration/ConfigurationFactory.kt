package de.pflugradts.passbird.application.configuration
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.failure.ConfigurationFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
class ConfigurationFactory constructor(
    private val systemOperation: SystemOperation,
    private val runContext: RunContext,
    private val configurationYamlMapper: ConfigurationYamlMapper = ConfigurationYamlMapper(),
) {
    fun loadConfiguration() = if (!systemOperation.exists(filePath)) {
        Configuration(template = true)
    } else {
        configurationFromFile()
    }
    private fun configurationFromFile() = tryCatching {
        filePath.let {
            it.toFile().inputStream().use(configurationYamlMapper::readConfiguration)
        }
    }.let { result ->
        result.exceptionOrNull()?.let {
            reportFailure(ConfigurationFailure(it))
            systemOperation.exit(FAILURE_EXIT_STATUS)
            throw it
        }
        result.getOrNull()!!
    }
    private val filePath get() = systemOperation.resolvePath(
        runContext.homeDirectory,
        CONFIGURATION_FILENAME.toFileName(),
    )
}
