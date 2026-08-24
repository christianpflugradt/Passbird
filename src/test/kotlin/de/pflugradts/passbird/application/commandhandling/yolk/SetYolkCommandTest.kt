package de.pflugradts.passbird.application.commandhandling.yolk

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.yolk.SetYolkCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.TestYolkData
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class SetYolkCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val setYolkCommandHandler =
        SetYolkCommandHandler(configuration, passwordService, userInterfaceAdapterPort, commandExecutionTracker)
    private val inputHandler = createInputHandlerFor(setYolkCommandHandler, commandExecutionTracker)

    @Test
    fun `should handle set yolk command with configured defaults`() {
        fakeConfiguration(
            instance = configuration,
            withYolkAlgorithm = "SHA256",
            withYolkDigits = 8,
            withYolkPeriodSeconds = 45,
        )
        fakePasswordService(instance = passwordService, withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("EggId"))))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("JBSWY3DPEHPK3PXP"))),
        )

        inputHandler.handleInput(inputOf(shellOf("y+EggId")))

        verify(exactly = 1) {
            passwordService.putYolk(
                eggIdShell = shellOf("EggId"),
                secretShell = any(),
                algorithm = "SHA256",
                digits = 8,
                periodSeconds = 45,
            )
        }
    }

    @Test
    fun `should handle set yolk command with existing yolk and confirmation`() {
        fakeConfiguration(instance = configuration)
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf("EggId"),
                    withYolk = TestYolkData(shellOf("before")),
                ),
            ),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withReceiveConfirmation = true,
            withTheseSecureInputs = listOf(inputOf(shellOf("JBSWY3DPEHPK3PXP"))),
        )

        inputHandler.handleInput(inputOf(shellOf("y+EggId")))

        verify(exactly = 1) { passwordService.putYolk(eq(shellOf("EggId")), any(), eq("SHA1"), eq(6), eq(30)) }
    }

    @Test
    fun `should abort set yolk command when overwrite confirmation is denied`() {
        fakeConfiguration(instance = configuration)
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf("EggId"),
                    withYolk = TestYolkData(shellOf("before")),
                ),
            ),
        )
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)

        inputHandler.handleInput(inputOf(shellOf("y+EggId")))

        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED)) }
        verify(exactly = 0) { passwordService.putYolk(eq(shellOf("EggId")), any(), any(), any(), any()) }
    }

    @Test
    fun `should abort set yolk command when secure input is unavailable`() {
        fakeConfiguration(instance = configuration)
        fakePasswordService(instance = passwordService, withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("EggId"))))
        every { userInterfaceAdapterPort.receiveSecurely(any()) } throws SecureInputUnavailableException()

        inputHandler.handleInput(inputOf(shellOf("y+EggId")))

        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED)) }
        verify(exactly = 0) { passwordService.putYolk(eq(shellOf("EggId")), any(), any(), any(), any()) }
    }

    @Test
    fun `should abort set yolk command on invalid secret`() {
        fakeConfiguration(instance = configuration)
        fakePasswordService(instance = passwordService, withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("EggId"))))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("!"))),
        )

        inputHandler.handleInput(inputOf(shellOf("y+EggId")))

        verify(exactly = 1) {
            userInterfaceAdapterPort.send(outputOf(shellOf("Invalid Base32 secret - Operation aborted."), OPERATION_ABORTED))
        }
        verify(exactly = 0) { passwordService.putYolk(eq(shellOf("EggId")), any(), any(), any(), any()) }
    }
}
