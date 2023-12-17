package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ExportCommandIT {

    private val importExportService = mockk<ImportExportService>(relaxed = true)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val exportCommandHandler = ExportCommandHandler(importExportService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(exportCommandHandler)

    @Test
    fun `should handle export command`() {
        // given
        val shell = shellOf("e")
        val reference = shell.copy()

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.exportEggs() }
    }
}
