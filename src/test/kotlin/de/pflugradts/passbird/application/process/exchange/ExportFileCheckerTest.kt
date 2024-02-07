package de.pflugradts.passbird.application.process.exchange

import de.pflugradts.passbird.application.Global
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.mainMocked
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExportFileCheckerTest {

    private val configuration = mockk<Configuration>(relaxed = true)
    private val systemOperation = mockk<SystemOperation>(relaxed = true)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val exportFileChecker = ExportFileChecker(configuration, systemOperation, userInterfaceAdapterPort)

    @BeforeEach
    fun setup() { mainMocked(arrayOf("/tmp")) }

    @Test
    fun `should delete export file if confirmed`() {
        // given
        val exportFile = fakePath(exists = true)
        fakeConfiguration(instance = configuration, withPromptOnExportFile = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveYes = true)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                Global.homeDirectory,
                ReadableConfiguration.EXCHANGE_FILENAME.toFileName(),
                exportFile,
            ),
        )

        // when
        exportFileChecker.run()

        // then
        verify(exactly = 1) { systemOperation.delete(exportFile) }
    }

    @Test
    fun `should not delete export file if not confirmed`() {
        // given
        val exportFile = fakePath(exists = true)
        fakeConfiguration(instance = configuration, withPromptOnExportFile = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveYes = false)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                Global.homeDirectory,
                ReadableConfiguration.EXCHANGE_FILENAME.toFileName(),
                exportFile,
            ),
        )

        // when
        exportFileChecker.run()

        // then
        verify(exactly = 0) { systemOperation.delete(exportFile) }
    }

    @Test
    fun `should not offer to delete file if it does not exist`() {
        // given
        val exportFile = fakePath(exists = false)
        fakeConfiguration(instance = configuration, withPromptOnExportFile = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveYes = true)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                Global.homeDirectory,
                ReadableConfiguration.EXCHANGE_FILENAME.toFileName(),
                exportFile,
            ),
        )

        // when
        exportFileChecker.run()

        // then
        verify(exactly = 0) { systemOperation.delete(exportFile) }
    }

    @Test
    fun `should not offer to delete file if it configuration parameter is not enabled`() {
        // given
        val exportFile = fakePath(exists = true)
        fakeConfiguration(instance = configuration, withPromptOnExportFile = false)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                Global.homeDirectory,
                ReadableConfiguration.EXCHANGE_FILENAME.toFileName(),
                exportFile,
            ),
        )

        // when
        exportFileChecker.run()

        // then
        verify(exactly = 0) { systemOperation.delete(exportFile) }
    }
}
