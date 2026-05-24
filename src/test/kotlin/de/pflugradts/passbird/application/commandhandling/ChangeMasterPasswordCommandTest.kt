package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ChangeMasterPasswordCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.security.KeyStoreAuthenticationService
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.emptyInput
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import java.nio.file.Path

@Tag(INTEGRATION)
class ChangeMasterPasswordCommandTest {

    private val keyStoreAdapterPort = mockk<KeyStoreAdapterPort>()
    private val keyStoreAuthenticationService = mockk<KeyStoreAuthenticationService>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val changeMasterPasswordCommandHandler = ChangeMasterPasswordCommandHandler(
        keyStoreAdapterPort = keyStoreAdapterPort,
        keyStoreAuthenticationService = keyStoreAuthenticationService,
        userInterfaceAdapterPort = userInterfaceAdapterPort,
    )
    private val inputHandler = createInputHandlerFor(changeMasterPasswordCommandHandler)

    @Test
    fun `should abort master password change when current password authentication fails`() {
        // given
        every { keyStoreAuthenticationService.authenticate(any()) } returns failure(RuntimeException())

        // when
        inputHandler.handleInput(inputOf(shellOf("k")))

        // then
        verify(exactly = 0) { keyStoreAdapterPort.storeExistingKey(any(), any(), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED)) }
    }

    @Test
    fun `should abort master password change when first new password input is empty`() {
        // given
        val key = shellOf("existing-key")
        val reference = key.copy()
        every { keyStoreAuthenticationService.authenticate(any()) } returns success(key)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(emptyInput()),
        )

        // when
        inputHandler.handleInput(inputOf(shellOf("k")))

        // then
        verify(exactly = 0) { keyStoreAdapterPort.storeExistingKey(any(), any(), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED)) }
        expectThat(key) isNotEqualTo reference
    }

    @Test
    fun `should abort master password change when second new password input is empty`() {
        // given
        val key = shellOf("existing-key")
        every { keyStoreAuthenticationService.authenticate(any()) } returns success(key)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("new-password")), emptyInput()),
        )

        // when
        inputHandler.handleInput(inputOf(shellOf("k")))

        // then
        verify(exactly = 0) { keyStoreAdapterPort.storeExistingKey(any(), any(), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED)) }
    }

    @Test
    fun `should repeat only the new password prompts after mismatched inputs`() {
        // given
        val key = shellOf("existing-key")
        val path = fakePath()
        val passwordSlot = slot<PlainShell>()
        every { keyStoreAuthenticationService.authenticate(any()) } returns success(key)
        every { keyStoreAuthenticationService.keyStorePath() } returns path
        every { keyStoreAdapterPort.storeExistingKey(eq(key), capture(passwordSlot), eq(path)) } returns Unit
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(
                inputOf(shellOf("new-password-1")),
                inputOf(shellOf("new-password-2")),
                inputOf(shellOf("new-password-3")),
                inputOf(shellOf("new-password-3")),
            ),
        )

        // when
        inputHandler.handleInput(inputOf(shellOf("k")))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Your inputs do not match, please repeat."))) }
        verify(exactly = 1) { keyStoreAdapterPort.storeExistingKey(eq(key), any(), eq(path)) }
        expectThat(passwordSlot.captured.toCharArray()) isEqualTo "new-password-3".toCharArray()
    }
}
