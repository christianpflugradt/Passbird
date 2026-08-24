package de.pflugradts.passbird.application.commandhandling.yolk

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.yolk.ViewYolkCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.egg.TestYolkData
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@Tag(INTEGRATION)
class ViewYolkCommandTest {

    private val configuration = mockk<Configuration>()
    private val clipboardAdapterPort = mockk<ClipboardAdapterPort>(relaxed = true)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val systemOperation = mockk<SystemOperation>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val viewYolkCommandHandler = ViewYolkCommandHandler(
        configuration,
        passwordService,
        clipboardAdapterPort,
        userInterfaceAdapterPort,
        systemOperation,
        commandExecutionTracker,
    )
    private val inputHandler = createInputHandlerFor(viewYolkCommandHandler, commandExecutionTracker)

    @Test
    fun `should show current yolk code immediately even when current one is near expiry`() {
        fakeConfiguration(instance = configuration)
        fakeSystemOperation(instance = systemOperation, withClock = Clock.fixed(Instant.parse("1970-01-01T00:00:59Z"), ZoneOffset.UTC))
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf("egg"),
                    withYolk = TestYolkData(shellOf("12345678901234567890")),
                ),
            ),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit
        every { userInterfaceAdapterPort.receiveLineBreakWithin(any()) } returnsMany listOf(false, true)

        inputHandler.handleInput(inputOf(shellOf("yegg")))

        expectThat(outputs.size) isEqualTo 1
        expectThat(outputs.first().shell.asString()) isEqualTo "Press Enter to return."
        verify(exactly = 1) { userInterfaceAdapterPort.startEphemeralLine(any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.updateEphemeralLine(any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.finishEphemeralLine() }
        verify(exactly = 1) { clipboardAdapterPort.post(any()) }
        verify(exactly = 0) { systemOperation.sleep(any()) }
    }

    @Test
    fun `should show current yolk code immediately when validity is high`() {
        fakeConfiguration(instance = configuration, withYolkCopyToClipboard = false)
        fakeSystemOperation(instance = systemOperation, withClock = Clock.fixed(Instant.parse("1970-01-01T00:00:50Z"), ZoneOffset.UTC))
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf("egg"),
                    withYolk = TestYolkData(shellOf("12345678901234567890")),
                ),
            ),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit
        every { userInterfaceAdapterPort.receiveLineBreakWithin(any()) } returns true

        inputHandler.handleInput(inputOf(shellOf("yegg")))

        expectThat(outputs.size) isEqualTo 1
        expectThat(outputs.first().shell.asString()) isEqualTo "Press Enter to return."
        verify(exactly = 1) { userInterfaceAdapterPort.startEphemeralLine(any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.updateEphemeralLine(any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.finishEphemeralLine() }
        verify(exactly = 0) { clipboardAdapterPort.post(any()) }
        verify(exactly = 0) { systemOperation.sleep(any()) }
    }

    @Test
    fun `should sync clipboard again when yolk code rolls over while view stays open`() {
        fakeConfiguration(instance = configuration)
        val mutableClock = object : Clock() {
            private var currentInstant = Instant.parse("1970-01-01T00:00:24Z")

            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId?): Clock = this

            override fun instant(): Instant = currentInstant

            fun advanceSeconds(seconds: Long) {
                currentInstant = currentInstant.plusSeconds(seconds)
            }
        }
        every { systemOperation.clock } returns mutableClock
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf("egg"),
                    withYolk = TestYolkData(shellOf("12345678901234567890")),
                ),
            ),
        )
        every { userInterfaceAdapterPort.receiveLineBreakWithin(any()) } answers {
            mutableClock.advanceSeconds(6)
            false
        } andThen true

        inputHandler.handleInput(inputOf(shellOf("yegg")))

        verify(exactly = 2) { clipboardAdapterPort.post(any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.updateEphemeralLine(any()) }
    }

    @Test
    fun `should abort when yolk is not found`() {
        fakeConfiguration(instance = configuration)
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = shellOf("egg")),
            ),
        )

        inputHandler.handleInput(inputOf(shellOf("yegg")))

        verify(exactly = 1) {
            userInterfaceAdapterPort.send(match { it.shell.asString() == "Yolk not found - Operation aborted." })
        }
        verify(exactly = 0) { userInterfaceAdapterPort.startEphemeralLine(any()) }
        verify(exactly = 0) { clipboardAdapterPort.post(any()) }
    }

    @Test
    fun `should only report missing egg when egg does not exist`() {
        fakeConfiguration(instance = configuration)
        fakePasswordService(instance = passwordService)

        inputHandler.handleInput(inputOf(shellOf("yegg")))

        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.startEphemeralLine(any()) }
        verify(exactly = 0) { clipboardAdapterPort.post(any()) }
    }
}
