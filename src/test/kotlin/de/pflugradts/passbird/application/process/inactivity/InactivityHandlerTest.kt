package de.pflugradts.passbird.application.process.inactivity

import de.pflugradts.passbird.application.commandhandling.CommandHandlerBus
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.commandhandling.command.QuitReason.INACTIVITY
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.time.Clock
import java.time.Instant

private const val FIVE_MINUTES = 5 * 60

class InactivityHandlerTest {

    private val instant = mockk<Instant>()
    private val commandBus = mockk<CommandHandlerBus>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val systemOperation = mockk<SystemOperation>()

    @BeforeEach
    fun setup() {
        val clock = mockk<Clock>()
        every { instant.epochSecond } returns 0
        every { clock.instant() } returns instant
        fakeSystemOperation(instance = systemOperation, withClock = clock)
        fakeConfiguration(instance = configuration, withInactivityTimeLimit = FIVE_MINUTES / 60)
    }

    @Test
    fun `should send quit command when inactivity limit is exceeded`() {
        // given
        every { instant.epochSecond } returns 0
        val inactivityHandler = InactivityHandler(commandBus, configuration, systemOperation)
        inactivityHandler.registerInteraction()
        val commandSlot = slot<QuitCommand>()

        // when
        every { instant.epochSecond } returns (FIVE_MINUTES + 1).toLong()
        inactivityHandler.checkInactivity()

        // then
        verify { commandBus.post(capture(commandSlot)) }
        expectThat(commandSlot.isCaptured).isTrue()
        expectThat(commandSlot.captured.quitReason) isEqualTo INACTIVITY
    }

    @Test
    fun `should not send quit command when inactivity limit is not exceeded`() {
        // given
        every { instant.epochSecond } returns 0
        val inactivityHandler = InactivityHandler(commandBus, configuration, systemOperation)
        inactivityHandler.registerInteraction()

        // when
        every { instant.epochSecond } returns (FIVE_MINUTES - 1).toLong()
        inactivityHandler.checkInactivity()

        // then
        verify { commandBus wasNot Called }
    }
}
