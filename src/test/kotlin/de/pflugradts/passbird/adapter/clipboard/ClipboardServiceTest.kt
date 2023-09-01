package de.pflugradts.passbird.adapter.clipboard

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Output
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.awaitility.Awaitility
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class ClipboardServiceTest {

    private val systemOperation = mockk<SystemOperation>()
    private val configuration = mockk<Configuration>()
    private val clipboardService = ClipboardService(systemOperation, configuration)

    @BeforeEach
    fun setup() {
        every { systemOperation.copyToClipboard(any()) } returns Unit
    }

    @Test
    fun `should copy message to clipboard`() {
        // given
        val message = "write this to clipboard"
        fakeConfiguration(
            instance = configuration,
            clipboardResetEnabled = false,
        )

        // when
        clipboardService.post(Output.of(Bytes.of(message)))

        // then
        verify(exactly = 1) { systemOperation.copyToClipboard(message) }
    }

    @Test
    fun `should clear clipboard`() {
        // given
        val message = "write this to clipboard"
        val delaySeconds = 1
        fakeConfiguration(
            instance = configuration,
            clipboardResetEnabled = true,
            clipboardResetDelaySeconds = delaySeconds,
        )

        // when
        clipboardService.post(Output.of(Bytes.of(message)))

        // then
        verify(exactly = 1) { systemOperation.copyToClipboard(message) }
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 1) { systemOperation.copyToClipboard("") }
        }
    }

    @Test
    fun `should reset clear timer`() {
        // given
        val message = "write this to clipboard"
        val anotherMessage = "write this next"
        val delaySeconds = 1
        val almostASecond = 800
        fakeConfiguration(
            instance = configuration,
            clipboardResetEnabled = true,
            clipboardResetDelaySeconds = delaySeconds,
        )

        // when
        clipboardService.post(Output.of(Bytes.of(message)))
        Thread.sleep(almostASecond.toLong())
        clipboardService.post(Output.of(Bytes.of(anotherMessage)))
        Thread.sleep(almostASecond.toLong())

        // then
        verify(exactly = 1) { systemOperation.copyToClipboard(message) }
        verify(exactly = 0) { systemOperation.copyToClipboard("") }
        verify(exactly = 1) { systemOperation.copyToClipboard(anotherMessage) }
        Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 1) { systemOperation.copyToClipboard("") }
        }
    }
}
