package de.pflugradts.passbird.application.configuration

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Paths

class ConfigurationSyncServiceTest {
    private val configuration = mockk<UpdatableConfiguration>(relaxed = true)
    private val systemOperation = mockk<SystemOperation>()
    private val configurationSyncService = ConfigurationSyncService(configuration, systemOperation)

    @Test
    fun `should return failure when configuration can not be written`() {
        val configurationDirectory = "tmp".toDirectory()
        every {
            systemOperation.resolvePath(configurationDirectory, CONFIGURATION_FILENAME.toFileName())
        } returns Paths.get("tmp", CONFIGURATION_FILENAME)
        every { systemOperation.writeToSensitiveFile(any(), any()) } throws IOException("disk full")
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        val actual = captureSystemErr.during {
            configurationSyncService.sync(configurationDirectory)
        }

        expectThat(actual.failure).isTrue()
        expectThat(captureSystemErr.capture) contains "Configuration could not be loaded: disk full"
        verify(exactly = 1) { configuration.updateDirectory(configurationDirectory) }
    }

    @Test
    fun `should persist repaired key store location to active configuration file`() {
        val configurationDirectory = "home".toDirectory()
        val keyStoreDirectory = "tmp".toDirectory()
        val repairedConfiguration = Configuration(
            application = Configuration.Application(
                backup = Configuration.Backup(location = "backups/custom"),
            ),
            adapter = Configuration.Adapter(
                keyStore = Configuration.KeyStore(location = "configured"),
                passwordTree = Configuration.PasswordTree(location = "tree"),
            ),
        )
        val configurationPath = Paths.get("home", CONFIGURATION_FILENAME)
        val service = ConfigurationSyncService(repairedConfiguration, systemOperation)
        every {
            systemOperation.resolvePath(configurationDirectory, CONFIGURATION_FILENAME.toFileName())
        } returns configurationPath
        every { systemOperation.writeToSensitiveFile(configurationPath, any()) } answers {
            secondArg<(java.io.OutputStream) -> Unit>().invoke(ByteArrayOutputStream())
            configurationPath
        }

        val actual = service.syncKeyStoreLocation(configurationDirectory, keyStoreDirectory)

        expectThat(actual.success).isTrue()
        expectThat(repairedConfiguration.adapter.keyStore.location).isEqualTo(keyStoreDirectory.value)
        expectThat(repairedConfiguration.adapter.passwordTree.location).isEqualTo("tree")
        expectThat(repairedConfiguration.application.backup.location).isEqualTo("backups/custom")
    }
}
