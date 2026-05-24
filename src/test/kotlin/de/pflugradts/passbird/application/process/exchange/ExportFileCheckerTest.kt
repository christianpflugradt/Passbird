package de.pflugradts.passbird.application.process.exchange

import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class ExportFileCheckerTest {

    private val configuration = mockk<Configuration>(relaxed = true)
    private val systemOperation = mockk<SystemOperation>(relaxed = true)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val runContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
    private val exportFileChecker = ExportFileChecker(configuration, runContext, systemOperation, userInterfaceAdapterPort)

    @Test
    fun `should delete export file if confirmed`() {
        // given
        val exportFile = fakePath()
        fakeConfiguration(instance = configuration, withPromptOnExportFile = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveYes = true)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                runContext.homeDirectory,
                ReadableConfiguration.EXCHANGE_FILENAME.toFileName(),
                exportFile,
            ),
        )
        every { systemOperation.exists(exportFile) } returns true

        // when
        exportFileChecker.run()

        // then
        verify(exactly = 1) { systemOperation.delete(exportFile) }
    }

    @Test
    fun `should not delete export file if not confirmed`() {
        // given
        val exportFile = fakePath()
        fakeConfiguration(instance = configuration, withPromptOnExportFile = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveYes = false)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                runContext.homeDirectory,
                ReadableConfiguration.EXCHANGE_FILENAME.toFileName(),
                exportFile,
            ),
        )
        every { systemOperation.exists(exportFile) } returns true

        // when
        exportFileChecker.run()

        // then
        verify(exactly = 0) { systemOperation.delete(exportFile) }
    }

    @Test
    fun `should not offer to delete file if it does not exist`() {
        // given
        val exportFile = fakePath()
        fakeConfiguration(instance = configuration, withPromptOnExportFile = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveYes = true)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                runContext.homeDirectory,
                ReadableConfiguration.EXCHANGE_FILENAME.toFileName(),
                exportFile,
            ),
        )
        every { systemOperation.exists(exportFile) } returns false

        // when
        exportFileChecker.run()

        // then
        verify(exactly = 0) { systemOperation.delete(exportFile) }
    }

    @Test
    fun `should not offer to delete file if it configuration parameter is not enabled`() {
        // given
        val exportFile = fakePath()
        fakeConfiguration(instance = configuration, withPromptOnExportFile = false)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                runContext.homeDirectory,
                ReadableConfiguration.EXCHANGE_FILENAME.toFileName(),
                exportFile,
            ),
        )
        every { systemOperation.exists(exportFile) } returns true

        // when
        exportFileChecker.run()

        // then
        verify(exactly = 0) { systemOperation.delete(exportFile) }
    }
}
