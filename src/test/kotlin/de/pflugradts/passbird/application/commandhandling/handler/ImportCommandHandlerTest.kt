package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ImportCommandHandlerTest {
    private val exchange = mockk<ImportExportService>()
    private val userInterface = mockk<UserInterfaceAdapterPort>()
    private val configuration = mockk<Configuration>()
    private val handler = ImportCommandHandler(
        configuration,
        exchange,
        createNestServiceForTesting(),
        mockk<PasswordService>(),
        userInterface,
        CommandExecutionTracker(),
    )
    private val inputHandler = createInputHandlerFor(handler, CommandExecutionTracker())

    @Test
    fun importsWithTheSecurelyEnteredPassword() {
        fakeConfiguration(configuration, withPromptOnRemoval = false)
        fakeUserInterfaceAdapterPort(userInterface, withTheseSecureInputs = listOf(inputOf(shellOf("import-password"))))
        var capturedPassword = charArrayOf()
        every { exchange.importEggs(any<CharArray>()) } answers {
            capturedPassword = firstArg<CharArray>().copyOf()
        }

        inputHandler.handleInput(inputOf(shellOf("i")))

        expectThat(capturedPassword.concatToString()) isEqualTo "import-password"
    }
}
