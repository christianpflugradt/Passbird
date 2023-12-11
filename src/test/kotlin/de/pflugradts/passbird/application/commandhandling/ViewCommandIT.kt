package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ViewCommandHandler
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class ViewCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val viewCommandHandler = ViewCommandHandler(passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(viewCommandHandler)

    @Test
    fun `should handle view command`() {
        // given
        val key = "key"
        val password = "password"
        val command = bytesOf("v$key")
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withKeyBytes = bytesOf(key), withPasswordBytes = bytesOf(password))),
        )
        val outputSlot = slot<Output>()

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.bytes.asString()) isEqualTo password
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should handle view command for non existing password`() {
        // given
        val key = "key"
        val command = bytesOf("v$key")
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = emptyList(),
        )

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }
}
