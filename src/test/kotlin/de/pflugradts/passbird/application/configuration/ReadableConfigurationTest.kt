package de.pflugradts.passbird.application.configuration

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.posixPermissionsIfSupported
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.util.UUID

@Tag(INTEGRATION)
class ReadableConfigurationTest {
    private val systemOperation = spyk(SystemOperation())
    private lateinit var configurationFactory: ConfigurationFactory
    private var tempConfigurationDirectory = UUID.randomUUID().toString()
    private var configurationFile = tempConfigurationDirectory + File.separator + CONFIGURATION_FILENAME

    @BeforeEach
    fun setup() {
        expectThat(File(tempConfigurationDirectory).mkdir()).isTrue()
        configurationFactory = ConfigurationFactory(
            systemOperation,
            PassbirdRunContext(tempConfigurationDirectory.toDirectory(), Slot.DEFAULT),
        )
    }

    @AfterEach
    fun cleanup() {
        expectThat(!File(configurationFile).exists() || File(configurationFile).delete()).isTrue()
        expectThat(File(tempConfigurationDirectory).delete()).isTrue()
    }

    @Test
    fun `should read, write, and read configuration again`() {
        // first load template if physical files does not exist
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()
        val configuration = captureSystemErr.during { configurationFactory.loadConfiguration() }
        expectThat(configuration.template).isTrue()
        expectThat(captureSystemErr.capture).isEqualTo("")

        // now persist configuration to file system
        expectThat(
            ConfigurationSyncService(configuration, systemOperation).sync(tempConfigurationDirectory.toDirectory()).success,
        ).isTrue()
        posixPermissionsIfSupported(Paths.get(configurationFile))?.let {
            expectThat(it) isEqualTo setOf(OWNER_READ, OWNER_WRITE)
        }

        // now load the persisted configuration and ensure the given configuration directory has been persisted too
        val loadedConfiguration = configurationFactory.loadConfiguration()
        expectThat(loadedConfiguration.adapter.keyStore.location) isEqualTo tempConfigurationDirectory
        expectThat(loadedConfiguration.template).isFalse()
    }

    @Test
    fun `should terminate instead of loading template when configuration contains unsupported properties`() {
        every { systemOperation.exit(any()) } returns Unit
        File(configurationFile).writeText(
            """
            application:
              unsupported: true
            """.trimIndent(),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            tryCatching { configurationFactory.loadConfiguration() }
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains "Configuration contains unrecognized property and will not be used."
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should terminate when protein templates have duplicate names`() {
        every { systemOperation.exit(any()) } returns Unit
        File(configurationFile).writeText(
            """
            domain:
              protein:
                templates:
                  - name: web-login
                    0: domain
                  - name: web-login
                    1: user
            """.trimIndent(),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            tryCatching { configurationFactory.loadConfiguration() }
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains "Configuration could not be loaded: Protein template names must be unique"
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should terminate when protein template name is blank`() {
        every { systemOperation.exit(any()) } returns Unit
        File(configurationFile).writeText(
            """
            domain:
              protein:
                templates:
                  - name: " "
                    0: domain
            """.trimIndent(),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            tryCatching { configurationFactory.loadConfiguration() }
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains "Configuration could not be loaded: Protein template name is invalid"
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should terminate when protein template slot is out of range`() {
        every { systemOperation.exit(any()) } returns Unit
        File(configurationFile).writeText(
            """
            domain:
              protein:
                templates:
                  - name: web-login
                    10: invalid
            """.trimIndent(),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            tryCatching { configurationFactory.loadConfiguration() }
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains
            "Configuration could not be loaded: Protein template 'web-login' contains invalid slot"
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should terminate when protein template slot key is not numeric`() {
        every { systemOperation.exit(any()) } returns Unit
        File(configurationFile).writeText(
            """
            domain:
              protein:
                templates:
                  - name: web-login
                    slot: invalid
            """.trimIndent(),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            tryCatching { configurationFactory.loadConfiguration() }
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains
            "Configuration could not be loaded: Protein template 'web-login' contains invalid slot"
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should terminate when yolk algorithm is invalid`() {
        every { systemOperation.exit(any()) } returns Unit
        File(configurationFile).writeText(
            """
            application:
              yolk:
                algorithm: SHA999
            """.trimIndent(),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            tryCatching { configurationFactory.loadConfiguration() }
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains "Configuration could not be loaded: Unsupported algorithm"
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should terminate when yolk digits are invalid`() {
        every { systemOperation.exit(any()) } returns Unit
        File(configurationFile).writeText(
            """
            application:
              yolk:
                digits: 7
            """.trimIndent(),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            tryCatching { configurationFactory.loadConfiguration() }
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains "Configuration could not be loaded: Yolk digits are invalid"
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }

    @Test
    fun `should terminate when yolk period is invalid`() {
        every { systemOperation.exit(any()) } returns Unit
        File(configurationFile).writeText(
            """
            application:
              yolk:
                periodSeconds: 0
            """.trimIndent(),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            tryCatching { configurationFactory.loadConfiguration() }
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains "Configuration could not be loaded: Yolk period is invalid"
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }
}
