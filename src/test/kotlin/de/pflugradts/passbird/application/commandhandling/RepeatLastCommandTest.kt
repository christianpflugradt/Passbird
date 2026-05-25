package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.RepeatLastCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.ViewCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.memory.UseMemoryCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.AddNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.verify
import jakarta.inject.Provider
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly

@Tag(INTEGRATION)
class RepeatLastCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val rememberedCommandMemory = RememberedCommandMemory()
    private lateinit var inputHandler: InputHandler

    @Test
    fun `should report that there is no previous command to repeat`() {
        inputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    RepeatLastCommandHandler(inputHandlerProvider(), rememberedCommandMemory, userInterfaceAdapterPort),
                ),
            ),
            rememberedCommandMemory,
        )

        inputHandler.handleInput(inputOf(shellOf(".")))

        verify(exactly = 1) {
            userInterfaceAdapterPort.send(
                outputOf(shellOf("No previous command is available to repeat."), OPERATION_ABORTED),
            )
        }
    }

    @Test
    fun `should repeat the last successful non-repeat command multiple times`() {
        val passwordService = mockk<PasswordService>()
        val nestService = createNestServiceForTesting()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = shellOf("Miro")),
                createEggForTesting(withEggIdShell = shellOf("Mail")),
                createEggForTesting(withEggIdShell = shellOf("miroBoard")),
            ),
            withNestService = nestService,
        )
        val outputs = mutableListOf<Output>()
        inputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    ListCommandHandler(nestService, passwordService, userInterfaceAdapterPort),
                    RepeatLastCommandHandler(inputHandlerProvider(), rememberedCommandMemory, userInterfaceAdapterPort),
                ),
            ),
            rememberedCommandMemory,
        )

        inputHandler.handleInput(inputOf(shellOf("lmiro")))
        inputHandler.handleInput(inputOf(shellOf(".")))
        inputHandler.handleInput(inputOf(shellOf(".")))

        verify(exactly = 3) { userInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs.map { it.shell.asString() }).containsExactly("Miro, miroBoard", "Miro, miroBoard", "Miro, miroBoard")
    }

    @Test
    fun `should repeat the original forwarded memory command instead of its expanded form`() {
        val passwordService = mockk<PasswordService>()
        val memory = linkedMapOf(DEFAULT to "email")
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = shellOf("email"), withPasswordShell = shellOf("secret-one")),
                createEggForTesting(withEggIdShell = shellOf("calendar"), withPasswordShell = shellOf("secret-two")),
            ),
            withMemory = memory,
        )
        val outputs = mutableListOf<Output>()
        val delegatingInputHandler = DelegatingInputHandler()
        inputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    RepeatLastCommandHandler(inputHandlerProvider(), rememberedCommandMemory, userInterfaceAdapterPort),
                    UseMemoryCommandHandler(delegatingInputHandler, passwordService, userInterfaceAdapterPort),
                    ViewCommandHandler(passwordService, userInterfaceAdapterPort),
                ),
            ),
            rememberedCommandMemory,
        )
        delegatingInputHandler.delegate = inputHandler

        inputHandler.handleInput(inputOf(shellOf("m0v")))
        memory[DEFAULT] = "calendar"
        inputHandler.handleInput(inputOf(shellOf(".")))

        verify(exactly = 2) { userInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs.map { it.shell.asString() }).containsExactly("secret-one", "secret-two")
    }

    @Test
    fun `should not remember an aborted command`() {
        val addNestUserInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
        val addNestRememberedCommandMemory = RememberedCommandMemory()
        val nestService = createNestServiceForTesting()
        lateinit var addNestInputHandler: InputHandler
        fakeUserInterfaceAdapterPort(
            instance = addNestUserInterfaceAdapterPort,
            withTheseInputs = listOf(Input.emptyInput()),
        )
        addNestInputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    AddNestCommandHandler(nestService, addNestUserInterfaceAdapterPort),
                    RepeatLastCommandHandler(
                        object : Provider<InputHandler> {
                            override fun get() = addNestInputHandler
                        },
                        addNestRememberedCommandMemory,
                        addNestUserInterfaceAdapterPort,
                    ),
                ),
            ),
            addNestRememberedCommandMemory,
        )
        val outputs = mutableListOf<Output>()

        addNestInputHandler.handleInput(inputOf(shellOf("n+1")))
        addNestInputHandler.handleInput(inputOf(shellOf(".")))

        verify(atLeast = 2) { addNestUserInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs.map { it.shell.asString() }).contains("Empty input - Operation aborted.")
        expectThat(outputs.map { it.shell.asString() }).contains("No previous command is available to repeat.")
    }

    private fun inputHandlerProvider() = object : Provider<InputHandler> {
        override fun get() = inputHandler
    }
}

private class DelegatingInputHandler : InputHandler {
    lateinit var delegate: InputHandler

    override fun handleInput(input: Input) = delegate.handleInput(input)
}
