package de.pflugradts.passbird.application.commandhandling.yolk

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.yolk.DiscardYolkCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.TestYolkData
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class DiscardYolkCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val discardYolkCommandHandler = DiscardYolkCommandHandler(passwordService, userInterfaceAdapterPort, commandExecutionTracker)
    private val inputHandler = createInputHandlerFor(discardYolkCommandHandler, commandExecutionTracker)

    @Test
    fun `should abort discard yolk command when yolk does not exist`() {
        fakePasswordService(instance = passwordService, withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("EggId"))))

        inputHandler.handleInput(inputOf(shellOf("y-EggId")))

        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Yolk not found - Operation aborted."), OPERATION_ABORTED)) }
    }

    @Test
    fun `should abort discard yolk command when confirmation is denied`() {
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("EggId"), withYolk = TestYolkData(shellOf("secret")))),
        )
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)

        inputHandler.handleInput(inputOf(shellOf("y-EggId")))

        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED)) }
        verify(exactly = 0) { passwordService.discardYolk(any()) }
    }

    @Test
    fun `should discard yolk command when confirmation is granted`() {
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("EggId"), withYolk = TestYolkData(shellOf("secret")))),
        )
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)

        inputHandler.handleInput(inputOf(shellOf("y-EggId")))

        verify(exactly = 1) { passwordService.discardYolk(shellOf("EggId")) }
    }
}
