package de.pflugradts.passbird.application.process.inactivity

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import io.kotest.assertions.nondeterministic.eventually
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class InactivityHandlerSchedulerTest {
    @Test
    fun `should schedule inactivity checks when inactivity monitoring is enabled`() {
        runBlocking {
            // given
            val configuration = mockk<Configuration>()
            val inactivityHandler = mockk<InactivityHandler>(relaxed = true)
            fakeConfiguration(instance = configuration, withInactivityTimeLimit = 5)

            // when
            InactivityHandlerScheduler(configuration, inactivityHandler).run()

            // then
            eventually(1.seconds) {
                verify(atLeast = 1) { inactivityHandler.checkInactivity() }
            }
        }
    }

    @Test
    fun `should not schedule inactivity checks when inactivity monitoring is disabled`() {
        // given
        val configuration = mockk<Configuration>()
        val inactivityHandler = mockk<InactivityHandler>(relaxed = true)
        fakeConfiguration(instance = configuration, withInactivityTimeLimit = 0)

        // when
        InactivityHandlerScheduler(configuration, inactivityHandler).run()

        // then
        Thread.sleep(100)
        verify(exactly = 0) { inactivityHandler.checkInactivity() }
    }
}
