package de.pflugradts.passbird.application.boot.main

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.mainMocked
import de.pflugradts.passbird.application.process.inactivity.InactivityHandler
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.fakeInput
import de.pflugradts.passbird.domain.service.nest.createNestServiceSpyForTesting
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class PassbirdApplicationTest {

    private val configuration = mockk<Configuration>()
    private val inactivityHandler = mockk<InactivityHandler>(relaxed = true)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val nestService = createNestServiceSpyForTesting()
    private val inputHandler = mockk<InputHandler>()
    private val passbirdApplication =
        PassbirdApplication(configuration, inactivityHandler, userInterfaceAdapterPort, nestService, inputHandler)

    @BeforeEach
    fun setup() {
        mainMocked(args = arrayOf("/tmp"), withMockedFileCheck = true)
        fakeConfiguration(instance = configuration)
    }

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
        val input1 = fakeInput("h")
        val interrupt = fakeInput(INTERRUPT)
        val givenNest = createNest(shellOf("mynest"), NestSlot.N1)
        val expectedNestPrefixSlot = mutableListOf<Output>()
        val expectedPromptPrefixSlot = mutableListOf<Output>()
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(input1, interrupt),
        )
        every { inputHandler.handleInput(any()) } returns Unit
        every { nestService.currentNest() } returns givenNest

        // when
        passbirdApplication.boot()

        // then
        verify { userInterfaceAdapterPort.receive(capture(expectedNestPrefixSlot), capture(expectedPromptPrefixSlot)) }
        expectThat(expectedNestPrefixSlot) hasSize 2
        expectedNestPrefixSlot.forEach { expectThat(it.shell.asString()) isEqualTo "[${givenNest.viewNestId().asString()}] " }
        expectedPromptPrefixSlot.forEach { expectThat(it.shell.asString()) isEqualTo "Enter command: " }
    }

    @Test
    fun `should display no nest if current is default`() {
        // given
        val input1 = fakeInput("h")
        val interrupt = fakeInput(INTERRUPT)
        val expectedNestPrefixSlot = mutableListOf<Output>()
        val expectedPromptPrefixSlot = mutableListOf<Output>()
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(input1, interrupt),
        )
        every { inputHandler.handleInput(any()) } returns Unit
        every { nestService.currentNest() } returns DEFAULT

        // when
        passbirdApplication.boot()

        // then
        verify { userInterfaceAdapterPort.receive(capture(expectedNestPrefixSlot), capture(expectedPromptPrefixSlot)) }
        expectThat(expectedNestPrefixSlot) hasSize 2
        expectedNestPrefixSlot.forEach { expectThat(it.shell.asString()) isEqualTo "" }
        expectedPromptPrefixSlot.forEach { expectThat(it.shell.asString()) isEqualTo "Enter command: " }
    }

    @Test
    fun `should set initial nest`() {
        // given
        val initialNestSlot = "5"
        val interrupt = fakeInput(INTERRUPT)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(interrupt),
        )
        every { inputHandler.handleInput(any()) } returns Unit

        // when
        mainMocked(args = arrayOf("/tmp", initialNestSlot), withMockedFileCheck = true)
        nestService.place(shellOf("nest5"), NestSlot.N5)
        passbirdApplication.boot()

        // then
        verify(exactly = 1) { nestService.moveToNestAt(nestSlotAt(initialNestSlot)) }
        expectThat(nestService.currentNest().nestSlot) isEqualTo nestSlotAt(initialNestSlot)
    }
}
