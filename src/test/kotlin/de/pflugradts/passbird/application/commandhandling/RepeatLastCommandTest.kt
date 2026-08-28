package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.GlobalHotkeyAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.RepeatLastCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.UseCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.ViewCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.memory.UseMemoryCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.AddNestCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.application.yolk.LiveYolkView
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
        val commandExecutionTracker = CommandExecutionTracker()
        inputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    RepeatLastCommandHandler(
                        inputHandlerProvider(),
                        rememberedCommandMemory,
                        userInterfaceAdapterPort,
                        commandExecutionTracker,
                    ),
                ),
            ),
            rememberedCommandMemory,
            commandExecutionTracker,
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
        val recordingUserInterfaceAdapterPort = RecordingUserInterfaceAdapterPort()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = shellOf("Miro")),
                createEggForTesting(withEggIdShell = shellOf("Mail")),
                createEggForTesting(withEggIdShell = shellOf("miroBoard")),
            ),
            withNestService = nestService,
        )
        val commandExecutionTracker = CommandExecutionTracker()
        inputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    ListCommandHandler(nestService, passwordService, recordingUserInterfaceAdapterPort),
                    RepeatLastCommandHandler(
                        inputHandlerProvider(),
                        rememberedCommandMemory,
                        recordingUserInterfaceAdapterPort,
                        commandExecutionTracker,
                    ),
                ),
            ),
            rememberedCommandMemory,
            commandExecutionTracker,
        )

        inputHandler.handleInput(inputOf(shellOf("lmiro")))
        inputHandler.handleInput(inputOf(shellOf(".")))
        inputHandler.handleInput(inputOf(shellOf(".")))

        expectThat(
            recordingUserInterfaceAdapterPort.sentOutputs.map { outputs ->
                outputs.joinToString(separator = "") { it.shell.asString() }
            },
        ).containsExactly("Miro, miroBoard", "Miro, miroBoard", "Miro, miroBoard")
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
        val renderedSecrets = mutableListOf<String>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } answers {
            renderedSecrets += outputs.last().shell.asString()
        }
        val delegatingInputHandler = DelegatingInputHandler()
        val commandExecutionTracker = CommandExecutionTracker()
        inputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    RepeatLastCommandHandler(
                        inputHandlerProvider(),
                        rememberedCommandMemory,
                        userInterfaceAdapterPort,
                        commandExecutionTracker,
                    ),
                    UseMemoryCommandHandler(
                        { delegatingInputHandler },
                        passwordService,
                        userInterfaceAdapterPort,
                        commandExecutionTracker,
                    ),
                    ViewCommandHandler(passwordService, userInterfaceAdapterPort, commandExecutionTracker),
                ),
            ),
            rememberedCommandMemory,
            commandExecutionTracker,
        )
        delegatingInputHandler.delegate = inputHandler

        inputHandler.handleInput(inputOf(shellOf("m0v")))
        memory[DEFAULT] = "calendar"
        inputHandler.handleInput(inputOf(shellOf(".")))

        verify(exactly = 2) { userInterfaceAdapterPort.send(any()) }
        expectThat(renderedSecrets).containsExactly("secret-one", "secret-two")
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
        val commandExecutionTracker = CommandExecutionTracker()
        addNestInputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    AddNestCommandHandler(nestService, addNestUserInterfaceAdapterPort, commandExecutionTracker),
                    RepeatLastCommandHandler(
                        { addNestInputHandler },
                        addNestRememberedCommandMemory,
                        addNestUserInterfaceAdapterPort,
                        commandExecutionTracker,
                    ),
                ),
            ),
            addNestRememberedCommandMemory,
            commandExecutionTracker,
        )
        val outputs = mutableListOf<Output>()

        addNestInputHandler.handleInput(inputOf(shellOf("n+1")))
        addNestInputHandler.handleInput(inputOf(shellOf(".")))

        verify(atLeast = 2) { addNestUserInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs.map { it.shell.asString() }).contains("Empty input - Operation aborted.")
        expectThat(outputs.map { it.shell.asString() }).contains("No previous command is available to repeat.")
    }

    @Test
    fun `should remember an aborted use command and repeat the full flow`() {
        val configuration = mockk<Configuration>()
        val passwordService = mockk<PasswordService>()
        val clipboardAdapterPort = mockk<ClipboardAdapterPort>()
        val globalHotkeyAdapterPort = mockk<GlobalHotkeyAdapterPort>()
        val useUserInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
        val systemOperation = mockk<SystemOperation>()
        val registerInteraction = mockk<() -> Unit>(relaxed = true)
        val rememberedCommandMemory = RememberedCommandMemory()
        val commandExecutionTracker = CommandExecutionTracker()
        fakeConfiguration(instance = configuration, withFlowGlobalHotkeyEnabled = false)
        fakeSystemOperation(instance = systemOperation)
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("egg"), withProteins = emptyMap())),
        )
        every { clipboardAdapterPort.post(any()) } returns failure(IllegalStateException("clipboard unavailable"))
        every { globalHotkeyAdapterPort.register(any()) } returns null
        val outputs = mutableListOf<Output>()
        every { useUserInterfaceAdapterPort.send(capture(outputs)) } returns Unit
        lateinit var useInputHandler: InputHandler
        useInputHandler = createInputHandlerFor(
            CommandHandlerBus(
                setOf(
                    UseCommandHandler(
                        configuration,
                        passwordService,
                        clipboardAdapterPort,
                        globalHotkeyAdapterPort,
                        useUserInterfaceAdapterPort,
                        registerInteraction,
                        LiveYolkView(configuration, clipboardAdapterPort, useUserInterfaceAdapterPort, systemOperation),
                        commandExecutionTracker,
                    ),
                    RepeatLastCommandHandler(
                        { useInputHandler },
                        rememberedCommandMemory,
                        useUserInterfaceAdapterPort,
                        commandExecutionTracker,
                    ),
                ),
            ),
            rememberedCommandMemory,
            commandExecutionTracker,
        )

        useInputHandler.handleInput(inputOf(shellOf("uegg")))
        useInputHandler.handleInput(inputOf(shellOf(".")))

        verify(exactly = 2) { clipboardAdapterPort.post(any()) }
        expectThat(outputs.map { it.shell.asString() }).containsExactly(
            "Password could not be copied to clipboard - Operation aborted.",
            "Password could not be copied to clipboard - Operation aborted.",
        )
    }

    private fun inputHandlerProvider() = { inputHandler }
}

private class DelegatingInputHandler : InputHandler {
    lateinit var delegate: InputHandler

    override fun handleInput(input: Input) = delegate.handleInput(input)
}

private class RecordingUserInterfaceAdapterPort : UserInterfaceAdapterPort {
    val sentOutputs = mutableListOf<List<Output>>()

    override fun receive(vararg output: Output) = error("not used in RepeatLastCommandTest")
    override fun receiveSecurely(output: Output) = error("not used in RepeatLastCommandTest")
    override fun receiveLineBreakWithin(milliseconds: Long) = error("not used in RepeatLastCommandTest")
    override fun send(vararg output: Output) {
        output.toList().takeUnless { outputs -> outputs.all { it.shell.isEmpty } }?.let(sentOutputs::add)
    }
    override fun startEphemeralLine(output: Output) = error("not used in RepeatLastCommandTest")
    override fun updateEphemeralLine(output: Output) = error("not used in RepeatLastCommandTest")
    override fun finishEphemeralLine() = error("not used in RepeatLastCommandTest")
    override fun warningSound() = error("not used in RepeatLastCommandTest")
}
