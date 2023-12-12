package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
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

internal class ListCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val listCommandHandler = ListCommandHandler(passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(listCommandHandler)

    @Test
    fun `should handle list command`() {
        // given
        val input = inputOf(bytesOf("l"))
        val eggId1 = bytesOf("eggId1")
        val eggId2 = bytesOf("eggId2")
        val eggId3 = bytesOf("eggId3")
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdBytes = eggId1),
                createEggForTesting(withEggIdBytes = eggId2),
                createEggForTesting(withEggIdBytes = eggId3),
            ),
        )
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.bytes.asString()) isEqualTo "${eggId1.asString()}, ${eggId2.asString()}, ${eggId3.asString()}"
    }

    @Test
    fun `should handle list command with empty database `() {
        // given
        val input = inputOf(bytesOf("l"))
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.bytes.asString()) isEqualTo "database is empty"
    }
}
