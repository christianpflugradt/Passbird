package de.pflugradts.passbird.application.configuration
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
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
) {
    fun loadConfiguration() = if (!systemOperation.exists(filePath)) {
        Configuration(template = true)
    } else {
        configurationFromFile()
    }
    private fun configurationFromFile() = tryCatching {
        filePath.let {
            YAMLMapper().readValue(
                it.toFile(),
                Configuration::class.java,
            ).validate()
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

private fun Configuration.validate(): Configuration {
    val names = domain.protein.templates.map { it.name }
    require(names.none { it.isBlank() }) { "Protein template name is invalid" }
    require(names.distinct().size == names.size) { "Protein template names must be unique" }
    domain.protein.templates.forEach { template ->
        require(template.slots.keys.all { it in 0..9 }) { "Protein template '${template.name}' contains invalid slot" }
    }
    return this
}
