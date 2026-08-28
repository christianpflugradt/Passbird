package de.pflugradts.passbird.application.commandhandling.egg

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.GlobalHotkeyAdapterPort
import de.pflugradts.passbird.application.RegisteredGlobalHotkey
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionOutcome
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.egg.UseCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.process.inactivity.InactivityHandler
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.application.yolk.LiveYolkView
import de.pflugradts.passbird.domain.model.egg.TestYolkData
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@Tag(INTEGRATION)
class UseCommandTest {

    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val clipboardAdapterPort = mockk<ClipboardAdapterPort>()
    private val globalHotkeyAdapterPort = mockk<GlobalHotkeyAdapterPort>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val systemOperation = mockk<SystemOperation>()
    private val inactivityHandler = spyk(
        InactivityHandler(
            mockk(relaxed = true),
            configuration,
            mockk(relaxed = true),
            systemOperation,
        ),
    )
    private val liveYolkView = LiveYolkView(configuration, clipboardAdapterPort, userInterfaceAdapterPort, systemOperation)
    private val commandExecutionTracker = CommandExecutionTracker()
    private val commandHandler = UseCommandHandler(
        configuration,
        passwordService,
        clipboardAdapterPort,
        globalHotkeyAdapterPort,
        userInterfaceAdapterPort,
        inactivityHandler,
        liveYolkView,
        commandExecutionTracker,
    )
    private val inputHandler = createInputHandlerFor(commandHandler, commandExecutionTracker)

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration)
        fakeSystemOperation(instance = systemOperation)
        every { clipboardAdapterPort.post(any()) } returns success(Unit)
        every { globalHotkeyAdapterPort.register(any()) } returns null
    }

    @Test
    fun `should guide through login password and yolk with hotkey and enter`() {
        val registeredHotkey = mockk<RegisteredGlobalHotkey>(relaxed = true)
        every { registeredHotkey.awaitWithin(any()) } returnsMany listOf(true, false)
        every { globalHotkeyAdapterPort.register('P') } returns registeredHotkey
        every { userInterfaceAdapterPort.receiveLineBreakWithin(any()) } returns true
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf("egg"),
                    withProteins = mapOf(DEFAULT to ShellPair(shellOf("login"), shellOf("alice@example.com"))),
                    withYolk = TestYolkData(shellOf("12345678901234567890")),
                ),
            ),
        )

        inputHandler.handleInput(inputOf(shellOf("uegg")))

        expectThat(outputs.map { it.shell.asString() }).containsExactly(
            "Ctrl+Shift+P",
            " or ",
            "Enter",
            " to continue",
            "",
            "Login",
            " copied to clipboard.",
            "",
            "Password",
            " copied to clipboard.",
            "Yolk",
            " copied to clipboard.",
        )
        verify(exactly = 1) { globalHotkeyAdapterPort.register('P') }
        verify(exactly = 1) { registeredHotkey.release() }
        verify(exactly = 2) { inactivityHandler.registerInteraction() }
        verify(exactly = 3) { clipboardAdapterPort.post(any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.startEphemeralLine(any()) }
        expectThat(commandExecutionTracker.lastCompletedOutcome()) isEqualTo CommandExecutionOutcome.SUCCESS
    }

    @Test
    fun `should return immediately for password only flow`() {
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("egg"), withProteins = emptyMap())),
        )

        inputHandler.handleInput(inputOf(shellOf("uegg")))

        expectThat(outputs.map { it.shell.asString() }).containsExactly("Password", " copied to clipboard.")
        verify(exactly = 0) { globalHotkeyAdapterPort.register(any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.receiveLineBreakWithin(any()) }
        expectThat(commandExecutionTracker.lastCompletedOutcome()) isEqualTo CommandExecutionOutcome.SUCCESS
    }

    @Test
    fun `should continue with enter when global hotkey registration fails`() {
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit
        every { userInterfaceAdapterPort.receiveLineBreakWithin(any()) } returns true
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf("egg"),
                    withProteins = mapOf(DEFAULT to ShellPair(shellOf("login"), shellOf("alice@example.com"))),
                ),
            ),
        )

        inputHandler.handleInput(inputOf(shellOf("uegg")))

        expectThat(outputs.map { it.shell.asString() }).containsExactly(
            "Global hotkey Ctrl+Shift+P could not be registered.",
            "Press Enter to continue.",
            "",
            "Login",
            " copied to clipboard.",
            "",
            "Password",
            " copied to clipboard.",
        )
        verify(exactly = 1) { userInterfaceAdapterPort.receiveLineBreakWithin(any()) }
    }

    @Test
    fun `should abort when clipboard copy fails`() {
        every { clipboardAdapterPort.post(any()) } returns failure(IllegalStateException("clipboard unavailable"))
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("egg"), withProteins = emptyMap())),
        )

        inputHandler.handleInput(inputOf(shellOf("uegg")))

        expectThat(outputs.map { it.shell.asString() }).containsExactly("Password could not be copied to clipboard - Operation aborted.")
        expectThat(commandExecutionTracker.lastCompletedOutcome()) isEqualTo CommandExecutionOutcome.ABORTED
    }

    @Test
    fun `should abort and release hotkey on unexpected error`() {
        val registeredHotkey = mockk<RegisteredGlobalHotkey>(relaxed = true)
        every { registeredHotkey.awaitWithin(any()) } returns false
        every { globalHotkeyAdapterPort.register('P') } returns registeredHotkey
        every { userInterfaceAdapterPort.receiveLineBreakWithin(any()) } throws IllegalStateException("stdin failed")
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf("egg"),
                    withProteins = mapOf(DEFAULT to ShellPair(shellOf("login"), shellOf("alice@example.com"))),
                ),
            ),
        )

        inputHandler.handleInput(inputOf(shellOf("uegg")))

        expectThat(outputs.map { it.shell.asString() }).containsExactly(
            "Ctrl+Shift+P",
            " or ",
            "Enter",
            " to continue",
            "",
            "Login",
            " copied to clipboard.",
            "Guided flow could not be completed - Operation aborted.",
        )
        verify(exactly = 1) { registeredHotkey.release() }
        expectThat(commandExecutionTracker.lastCompletedOutcome()) isEqualTo CommandExecutionOutcome.ABORTED
    }
}
