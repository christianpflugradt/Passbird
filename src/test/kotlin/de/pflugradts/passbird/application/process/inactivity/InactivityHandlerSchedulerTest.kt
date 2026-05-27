package de.pflugradts.passbird.application.process.inactivity

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import io.mockk.mockk
import io.mockk.verify
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.Duration

class InactivityHandlerSchedulerTest {
    @Test
    fun `should schedule inactivity checks when inactivity monitoring is enabled`() {
        // given
        val configuration = mockk<Configuration>()
        val inactivityHandler = mockk<InactivityHandler>(relaxed = true)
        fakeConfiguration(instance = configuration, withInactivityTimeLimit = 5)

        // when
        InactivityHandlerScheduler(configuration, inactivityHandler).run()

        // then
        await().atMost(Duration.ofSeconds(1)).untilAsserted {
            verify(atLeast = 1) { inactivityHandler.checkInactivity() }
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
