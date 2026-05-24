package de.pflugradts.passbird.application.process.inactivity

import de.pflugradts.passbird.application.commandhandling.CommandHandlerBus
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.commandhandling.command.QuitReason.INACTIVITY
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import io.mockk.Called
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
import java.time.ZoneId
import java.time.ZoneOffset

private const val FIVE_MINUTES = 5 * 60

class InactivityHandlerTest {

    private val commandBus = mockk<CommandHandlerBus>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val systemOperation = mockk<SystemOperation>()
    private val clock = AdjustableClock()

    @BeforeEach
    fun setup() {
        clock.setEpochSecond(0)
        fakeSystemOperation(instance = systemOperation, withClock = clock)
        fakeConfiguration(instance = configuration, withInactivityTimeLimit = FIVE_MINUTES / 60)
    }

    @Test
    fun `should send quit command when inactivity limit is exceeded`() {
        // given
        val inactivityHandler = InactivityHandler(commandBus, configuration, systemOperation)
        inactivityHandler.registerInteraction()
        val commandSlot = slot<QuitCommand>()

        // when
        clock.setEpochSecond((FIVE_MINUTES + 1).toLong())
        inactivityHandler.checkInactivity()

        // then
        verify { commandBus.post(capture(commandSlot)) }
        expectThat(commandSlot.isCaptured).isTrue()
        expectThat(commandSlot.captured.quitReason) isEqualTo INACTIVITY
    }

    @Test
    fun `should not send quit command when inactivity limit is not exceeded`() {
        // given
        val inactivityHandler = InactivityHandler(commandBus, configuration, systemOperation)
        inactivityHandler.registerInteraction()

        // when
        clock.setEpochSecond((FIVE_MINUTES - 1).toLong())
        inactivityHandler.checkInactivity()

        // then
        verify { commandBus wasNot Called }
    }

    private class AdjustableClock(
        private var currentInstant: Instant = Instant.EPOCH,
    ) : Clock() {
        fun setEpochSecond(epochSecond: Long) {
            currentInstant = Instant.ofEpochSecond(epochSecond)
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = currentInstant
    }
}
