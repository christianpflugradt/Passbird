package de.pflugradts.passbird.application.boot.main

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.fakeInput
import de.pflugradts.passbird.domain.service.NamespaceService
import de.pflugradts.passbird.domain.service.NamespaceServiceFake
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class PassbirdApplicationTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val namespaceServiceFake = spyk<NamespaceService>(NamespaceServiceFake())
    private val inputHandler = mockk<InputHandler>()
    private val passbirdApplication = PassbirdApplication(userInterfaceAdapterPort, namespaceServiceFake, inputHandler)

    @Test
    fun `should delegate input`() {
        // given
        val input1 = fakeInput("1")
        val input2 = fakeInput("2")
        val input3 = fakeInput("3")
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
    fun `should display namespace if current is other than default`() {
        // given
        val input1 = fakeInput("1")
        val interrupt = fakeInput(INTERRUPT)
        val givenNamespace = "mynamespace"
        val expectedOutput = mutableListOf<Output>()
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(input1, interrupt),
        )
        every { inputHandler.handleInput(any()) } returns Unit

        // when
        namespaceServiceFake.deploy(bytesOf(givenNamespace), NamespaceSlot._1)
        namespaceServiceFake.updateCurrentNamespace(NamespaceSlot._1)
        passbirdApplication.boot()

        // then
        verify { userInterfaceAdapterPort.receive(capture(expectedOutput)) }
        expectThat(expectedOutput.size == 2).isTrue()
        expectedOutput.forEach { expectThat(it.bytes.asString()) isEqualTo "[$givenNamespace] Enter command: " }
    }

    @Test
    fun `should display no namespace if current is default`() {
        // given
        val input1 = fakeInput("1")
        val interrupt = fakeInput(INTERRUPT)
        val expectedOutput = mutableListOf<Output>()
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(input1, interrupt),
        )
        every { inputHandler.handleInput(any()) } returns Unit

        // when
        namespaceServiceFake.updateCurrentNamespace(NamespaceSlot.DEFAULT)
        passbirdApplication.boot()

        // then
        verify { userInterfaceAdapterPort.receive(capture(expectedOutput)) }
        expectThat(expectedOutput.size == 2).isTrue()
        expectedOutput.forEach { expectThat(it.bytes.asString()) isEqualTo "Enter command: " }
    }
}
