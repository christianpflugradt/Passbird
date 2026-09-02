package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.provider.fakePasswordProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Tag(INTEGRATION)
class ExportCommandTest {

    @BeforeEach
    fun setup() {
        fakePasswordProvider(passwordProvider, shellOf("generated"))
        fakeUserInterfaceAdapterPort(userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(shellOf("y"))))
    }

    private val importExportService = mockk<ImportExportService>(relaxed = true)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val passwordProvider = mockk<PasswordProvider>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val exportCommandHandler = ExportCommandHandler(
        CanListAvailableNests(nestService),
        Configuration(),
        importExportService,
        nestService,
        passwordProvider,
        userInterfaceAdapterPort,
        commandExecutionTracker,
    )
    private val inputHandler = createInputHandlerFor(exportCommandHandler, commandExecutionTracker)

    @Test
    fun `should handle export command`() {
        // given
        val shell = shellOf("e")
        val reference = shell.copy()

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.exportEggs(any()) }
    }

    @Test
    fun `should handle selective export command for selected nests`() {
        // given
        nestService.place(shellOf("work"), S2)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("y")), inputOf(shellOf("1")), inputOf(shellOf("0,2"))),
        )

        // when
        inputHandler.handleInput(inputOf(shellOf("e*")))

        // then
        verify(exactly = 1) { importExportService.exportEggs(setOf(DEFAULT, S2), any()) }
    }

    @Test
    fun `should handle selective export command for all except selected nests`() {
        // given
        nestService.place(shellOf("work"), S2)
        nestService.place(shellOf("archive"), S5)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("y")), inputOf(shellOf("2")), inputOf(shellOf("2"))),
        )

        // when
        inputHandler.handleInput(inputOf(shellOf("e*")))

        // then
        verify(exactly = 1) { importExportService.exportEggs(setOf(DEFAULT, S5), any()) }
    }

    @Test
    fun `should abort selective export when selection resolves to no nests`() {
        // given
        nestService.place(shellOf("work"), S2)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("y")), inputOf(shellOf("2")), inputOf(shellOf("0,2"))),
        )

        // when
        inputHandler.handleInput(inputOf(shellOf("e*")))

        // then
        verify(exactly = 0) { importExportService.exportEggs(any<Set<de.pflugradts.passbird.domain.model.slot.Slot>>(), any()) }
    }

    @Test
    fun `should reject export command with trailing input`() {
        // given
        val captureSystemErr = captureSystemErr()

        // when
        captureSystemErr.during {
            inputHandler.handleInput(inputOf(shellOf("email")))
        }

        // then
        verify(exactly = 0) { importExportService.exportEggs(any()) }
        expectThat(captureSystemErr.capture) isEqualTo "Command execution failed: Parameter for command 'e' not supported: mail\n"
    }
}
