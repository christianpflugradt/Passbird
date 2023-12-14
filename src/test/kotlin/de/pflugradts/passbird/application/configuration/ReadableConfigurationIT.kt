package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_SYSTEM_PROPERTY
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.spyk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.util.UUID

class ReadableConfigurationIT {

    private val systemOperation = spyk(SystemOperation())
    private val configurationFactory = ConfigurationFactory(systemOperation)
    private var tempConfigurationDirectory = UUID.randomUUID().toString()
    private var configurationFile = tempConfigurationDirectory + File.separator + CONFIGURATION_FILENAME

    @BeforeEach
    fun setup() {
        expectThat(File(tempConfigurationDirectory).mkdir()).isTrue()
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(configurationFile).delete()).isTrue()
        expectThat(File(tempConfigurationDirectory).delete()).isTrue()
    }

    @Test
    fun `should read, write, and read configuration again`() {
        // first load template if physical files does not exist
        System.setProperty(CONFIGURATION_SYSTEM_PROPERTY, tempConfigurationDirectory)
        val configuration = configurationFactory.loadConfiguration()
        expectThat(configuration.template).isTrue()

        // now persist configuration to file system
        ConfigurationSyncService(configuration, systemOperation).sync(tempConfigurationDirectory.toDirectory())

        // now load the persisted configuration and ensure the given configuration directory has been persisted too
        val loadedConfiguration = configurationFactory.loadConfiguration()
        expectThat(loadedConfiguration.adapter.keyStore.location) isEqualTo tempConfigurationDirectory
        expectThat(loadedConfiguration.template).isFalse()
    }
}
