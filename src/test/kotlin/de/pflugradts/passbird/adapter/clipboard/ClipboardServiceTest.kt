package de.pflugradts.passbird.adapter.clipboard

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.TimeUnit

class ClipboardServiceTest {

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
        fakeConfiguration(instance = configuration)

        // when
        clipboardService.post(outputOf(shellOf(message)))

        // then
        verify(exactly = 1) { systemOperation.copyToClipboard(message) }
    }

    @Test
    fun `should report error on copy to clipboard`() {
        // given
        val message = "write this to clipboard"
        val error = "clipboard unavailable"
        fakeConfiguration(instance = configuration)
        every { systemOperation.copyToClipboard(message) } throws IllegalStateException(error)
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        captureSystemErr.during {
            clipboardService.post(outputOf(shellOf(message)))
        }

        // then
        val actual = captureSystemErr.capture
        expectThat(actual) isEqualTo "Clipboard could not be updated. Please check your Java version. Exception: $error\n"
    }

    @Test
    fun `should clear clipboard`() {
        // given
        val message = "write this to clipboard"
        val delaySeconds = 1
        fakeConfiguration(
            instance = configuration,
            withClipboardResetEnabled = true,
            withClipboardResetDelaySeconds = delaySeconds,
        )

        // when
        clipboardService.post(outputOf(shellOf(message)))

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
            withClipboardResetEnabled = true,
            withClipboardResetDelaySeconds = delaySeconds,
        )

        // when
        clipboardService.post(outputOf(shellOf(message)))
        Thread.sleep(almostASecond.toLong())
        clipboardService.post(outputOf(shellOf(anotherMessage)))
        Thread.sleep(almostASecond.toLong())

        // then
        verify(exactly = 1) { systemOperation.copyToClipboard(message) }
        verify(exactly = 0) { systemOperation.copyToClipboard("") }
        verify(exactly = 1) { systemOperation.copyToClipboard(anotherMessage) }
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 1) { systemOperation.copyToClipboard("") }
        }
    }
}
