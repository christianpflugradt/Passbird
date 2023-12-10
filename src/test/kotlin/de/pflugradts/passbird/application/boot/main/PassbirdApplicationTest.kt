package de.pflugradts.passbird.application.boot.main

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.fakeInput
import de.pflugradts.passbird.domain.service.createNestServiceSpyForTesting
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class PassbirdApplicationTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val nestService = createNestServiceSpyForTesting()
    private val inputHandler = mockk<InputHandler>()
    private val passbirdApplication = PassbirdApplication(userInterfaceAdapterPort, nestService, inputHandler)

    @Test
    fun `should delegate input`() {
        // given
        val input1 = fakeInput("n1")
        val input2 = fakeInput("vtest")
        val input3 = fakeInput("")
        val interrupt = fakeInput(INTERRUPT)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(input1, input2, input3, interrupt),
        )
        every { inputHandler.handleInput(any()) } returns Unit

        // when
        passbirdApplication.boot()

        // then
        listOf(
            input1,
            input2,
            input3,
        ).forEach { verify(exactly = 1) { inputHandler.handleInput(it) } }
    }

    @Test
    fun `should display nest if current is other than default`() {
        // given
        val input1 = fakeInput("1")
        val interrupt = fakeInput(INTERRUPT)
        val givenNest = "mynest"
        val expectedOutputSlot = mutableListOf<Output>()
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(input1, interrupt),
        )
        every { inputHandler.handleInput(any()) } returns Unit

        // when
        nestService.deploy(bytesOf(givenNest), Slot.N1)
        nestService.moveToNestAt(Slot.N1)
        passbirdApplication.boot()

        // then
        verify { userInterfaceAdapterPort.receive(capture(expectedOutputSlot)) }
        expectThat(expectedOutputSlot) hasSize 2
        expectedOutputSlot.forEach { expectThat(it.bytes.asString()) isEqualTo "[$givenNest] Enter command: " }
    }

    @Test
    fun `should display no nest if current is default`() {
        // given
        val input1 = fakeInput("1")
        val interrupt = fakeInput(INTERRUPT)
        val expectedOutputSlot = mutableListOf<Output>()
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(input1, interrupt),
        )
        every { inputHandler.handleInput(any()) } returns Unit

        // when
        nestService.moveToNestAt(Slot.DEFAULT)
        passbirdApplication.boot()

        // then
        verify { userInterfaceAdapterPort.receive(capture(expectedOutputSlot)) }
        expectThat(expectedOutputSlot) hasSize 2
        expectedOutputSlot.forEach { expectThat(it.bytes.asString()) isEqualTo "Enter command: " }
    }
}
