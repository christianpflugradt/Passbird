package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.provider.fakePasswordProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ExportCommandHandlerTest {
    private val exchange = mockk<ImportExportService>()
    private val userInterface = mockk<UserInterfaceAdapterPort>()
    private val nestService = createNestServiceForTesting()
    private val passwordProvider = mockk<PasswordProvider>()
    private val handler = ExportCommandHandler(
        CanListAvailableNests(nestService),
        Configuration(),
        exchange,
        nestService,
        passwordProvider,
        userInterface,
        CommandExecutionTracker(),
    )
    private val inputHandler = createInputHandlerFor(handler, CommandExecutionTracker())

    @Test
    fun exportsWithGeneratedPassword() {
        fakeUserInterfaceAdapterPort(userInterface, withTheseInputs = listOf(inputOf(shellOf(""))))
        fakePasswordProvider(passwordProvider, shellOf("generated"))
        var capturedPassword = charArrayOf()
        every { exchange.exportEggs(any<CharArray>()) } answers {
            capturedPassword = firstArg<CharArray>().copyOf()
            true
        }

        inputHandler.handleInput(inputOf(shellOf("e")))

        expectThat(capturedPassword.concatToString()) isEqualTo "generated"
        verify { userInterface.send(match { it.shell.asString() == "Your export password: generated" }) }
    }

    @Test
    fun abortsWhenManualPasswordIsEmpty() {
        fakeUserInterfaceAdapterPort(
            userInterface,
            withTheseInputs = listOf(inputOf(shellOf("n"))),
            withTheseSecureInputs = listOf(inputOf(shellOf("")), inputOf(shellOf(""))),
        )

        inputHandler.handleInput(inputOf(shellOf("e")))

        verify(exactly = 0) { exchange.exportEggs(any<CharArray>()) }
        verify { userInterface.send(match { it.shell.asString() == "Operation aborted." }) }
    }

    @Test
    fun reportsManualPasswordMismatchWithoutExporting() {
        fakeUserInterfaceAdapterPort(
            userInterface,
            withTheseInputs = listOf(inputOf(shellOf("n"))),
            withTheseSecureInputs = listOf(inputOf(shellOf("first")), inputOf(shellOf("second"))),
        )

        inputHandler.handleInput(inputOf(shellOf("e")))

        verify(exactly = 0) { exchange.exportEggs(any<CharArray>()) }
        verify { userInterface.send(match { it.shell.asString() == "Export passwords do not match. Operation aborted." }) }
    }

    @Test
    fun exportsWithMatchingManualPasswordWithoutDisplayingIt() {
        fakeUserInterfaceAdapterPort(
            userInterface,
            withTheseInputs = listOf(inputOf(shellOf("n"))),
            withTheseSecureInputs = listOf(inputOf(shellOf("manual")), inputOf(shellOf("manual"))),
        )
        var capturedPassword = charArrayOf()
        every { exchange.exportEggs(any<CharArray>()) } answers {
            capturedPassword = firstArg<CharArray>().copyOf()
            true
        }

        inputHandler.handleInput(inputOf(shellOf("e")))

        expectThat(capturedPassword.concatToString()) isEqualTo "manual"
        verify(exactly = 0) { userInterface.send(match { it.shell.asString().startsWith("Your export password:") }) }
    }

    @Test
    fun doesNotDisplayGeneratedPasswordWhenExportFails() {
        fakeUserInterfaceAdapterPort(userInterface, withTheseInputs = listOf(inputOf(shellOf(""))))
        fakePasswordProvider(passwordProvider, shellOf("generated"))
        every { exchange.exportEggs(any<CharArray>()) } returns false

        inputHandler.handleInput(inputOf(shellOf("e")))

        verify(exactly = 0) { userInterface.send(match { it.shell.asString().startsWith("Your export password:") }) }
    }

    @Test
    fun abortsForAnInvalidPasswordChoice() {
        fakeUserInterfaceAdapterPort(userInterface, withTheseInputs = listOf(inputOf(shellOf("invalid"))))

        inputHandler.handleInput(inputOf(shellOf("e")))

        verify(exactly = 0) { exchange.exportEggs(any<CharArray>()) }
        verify { userInterface.send(match { it.shell.asString() == "Operation aborted." }) }
    }

    @Test
    fun abortsSelectiveExportWithAnInvalidSelection() {
        fakeUserInterfaceAdapterPort(userInterface, withTheseInputs = listOf(inputOf(shellOf("")), inputOf(shellOf("invalid"))))
        fakePasswordProvider(passwordProvider, shellOf("generated"))

        inputHandler.handleInput(inputOf(shellOf("e*")))

        verify(exactly = 0) { exchange.exportEggs(any<Set<de.pflugradts.passbird.domain.model.slot.Slot>>(), any<CharArray>()) }
        verify { userInterface.send(match { it.shell.asString() == "Operation aborted." }) }
    }
}
