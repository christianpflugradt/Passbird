package de.pflugradts.passbird.application.process.hotkey

import de.pflugradts.passbird.application.GlobalHotkeyAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class GlobalHotkeyStartupCheckerTest {
    private val configuration = mockk<Configuration>()
    private val globalHotkeyAdapterPort = mockk<GlobalHotkeyAdapterPort>()
    private val systemOperation = mockk<SystemOperation>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()

    @Test
    fun `should do nothing when hotkey is disabled`() {
        fakeConfiguration(instance = configuration, withFlowGlobalHotkeyEnabled = false)
        fakeSystemOperation(instance = systemOperation)
        every { globalHotkeyAdapterPort.prepareOnStartup() } returns false
        every { userInterfaceAdapterPort.send(any()) } returns Unit
        every { userInterfaceAdapterPort.sendLineBreak() } returns Unit

        GlobalHotkeyStartupChecker(configuration, globalHotkeyAdapterPort, systemOperation, userInterfaceAdapterPort).run()

        verify(exactly = 0) { globalHotkeyAdapterPort.prepareOnStartup() }
        verify(exactly = 0) { systemOperation.exit(any()) }
    }

    @Test
    fun `should do nothing when startup preparation succeeds`() {
        fakeConfiguration(instance = configuration, withFlowGlobalHotkeyEnabled = true)
        fakeSystemOperation(instance = systemOperation)
        every { globalHotkeyAdapterPort.prepareOnStartup() } returns true
        every { userInterfaceAdapterPort.send(any()) } returns Unit
        every { userInterfaceAdapterPort.sendLineBreak() } returns Unit

        GlobalHotkeyStartupChecker(configuration, globalHotkeyAdapterPort, systemOperation, userInterfaceAdapterPort).run()

        verify(exactly = 1) { globalHotkeyAdapterPort.prepareOnStartup() }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        verify(exactly = 0) { systemOperation.exit(any()) }
    }

    @Test
    fun `should print approved message with blank lines and terminate when startup preparation fails`() {
        val output = slot<de.pflugradts.passbird.domain.model.transfer.Output>()
        fakeConfiguration(instance = configuration, withFlowGlobalHotkeyEnabled = true)
        fakeSystemOperation(instance = systemOperation)
        every { globalHotkeyAdapterPort.prepareOnStartup() } returns false
        every { userInterfaceAdapterPort.send(capture(output)) } returns Unit
        every { userInterfaceAdapterPort.sendLineBreak() } returns Unit

        GlobalHotkeyStartupChecker(configuration, globalHotkeyAdapterPort, systemOperation, userInterfaceAdapterPort).run()

        verify(exactly = 2) { userInterfaceAdapterPort.sendLineBreak() }
        expectThat(output.captured.shell.asString()).isEqualTo(
            "Global hotkey permission is required on macOS. Allow Input Monitoring for the terminal app that starts " +
                "Passbird, restart that terminal app, and then start Passbird again.",
        )
        verify(exactly = 1) { systemOperation.exit(FAILURE_EXIT_STATUS) }
    }
}
