package de.pflugradts.passbird.adapter.clipboard

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ClipboardServiceTest {

    private val clipboardGateway = mockk<ClipboardGateway>()
    private val configuration = mockk<Configuration>()
    private val scheduledTasks = mutableListOf<() -> Unit>()
    private val resetScheduler = ClipboardResetScheduler { delayMillis, task, onFailure ->
        if (delayMillis < 0) onFailure(IllegalArgumentException("timeout value is negative")) else scheduledTasks.add(task)
    }
    private val clipboardService = ClipboardService(clipboardGateway, configuration, resetScheduler)

    @BeforeEach
    fun setup() {
        every { clipboardGateway.copy(any(), any()) } returns Unit
        scheduledTasks.clear()
    }

    @Test
    fun `should copy message to clipboard`() {
        // given
        val message = "write this to clipboard"
        fakeConfiguration(instance = configuration)

        // when
        val actual = clipboardService.post(outputOf(shellOf(message)))

        // then
        expectThat(actual.success) isEqualTo true
        verify(exactly = 1) { clipboardGateway.copy(message, true) }
    }

    @Test
    fun `should report error on copy to clipboard`() {
        // given
        val message = "write this to clipboard"
        val error = "clipboard unavailable"
        fakeConfiguration(instance = configuration)
        every { clipboardGateway.copy(message, true) } throws IllegalStateException(error)
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        val actual = captureSystemErr.during {
            clipboardService.post(outputOf(shellOf(message)))
        }

        // then
        expectThat(actual.failure) isEqualTo true
        val errorOutput = captureSystemErr.capture
        expectThat(errorOutput) isEqualTo "Clipboard could not be updated. Please check your Java version. Exception: $error\n"
    }

    @Test
    fun `should clear clipboard`() {
        // given
        val message = "write this to clipboard"
        fakeConfiguration(
            instance = configuration,
            withClipboardResetEnabled = true,
        )

        // when
        clipboardService.post(outputOf(shellOf(message)))
        scheduledTasks.single().invoke()

        // then
        verify(exactly = 1) { clipboardGateway.copy(message, true) }
        verify(exactly = 1) { clipboardGateway.copy("", true) }
    }

    @Test
    fun `should reset clear timer`() {
        // given
        val message = "write this to clipboard"
        val anotherMessage = "write this next"
        fakeConfiguration(
            instance = configuration,
            withClipboardResetEnabled = true,
        )

        // when
        clipboardService.post(outputOf(shellOf(message)))
        clipboardService.post(outputOf(shellOf(anotherMessage)))
        scheduledTasks.first().invoke()

        // then
        verify(exactly = 1) { clipboardGateway.copy(message, true) }
        verify(exactly = 0) { clipboardGateway.copy("", true) }
        verify(exactly = 1) { clipboardGateway.copy(anotherMessage, true) }

        // when
        scheduledTasks.last().invoke()

        // then
        verify(exactly = 1) { clipboardGateway.copy("", true) }
    }

    @Test
    fun `should report error when delayed clipboard clear fails`() {
        // given
        val message = "write this to clipboard"
        val error = "clipboard clear unavailable"
        fakeConfiguration(
            instance = configuration,
            withClipboardResetEnabled = true,
            withClipboardResetDelaySeconds = 0,
        )
        every { clipboardGateway.copy("", true) } throws IllegalStateException(error)
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()
        val expectedError = "Clipboard could not be updated. Please check your Java version. Exception: $error\n"

        // when
        captureSystemErr.during {
            clipboardService.post(outputOf(shellOf(message)))
            scheduledTasks.single().invoke()
        }

        // then
        expectThat(captureSystemErr.capture) isEqualTo expectedError
    }

    @Test
    fun `should report error when clipboard reset delay is invalid`() {
        // given
        val message = "write this to clipboard"
        fakeConfiguration(
            instance = configuration,
            withClipboardResetEnabled = true,
            withClipboardResetDelaySeconds = -1,
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()
        val expectedError = "Clipboard could not be updated. Please check your Java version. Exception: timeout value is negative\n"

        // when
        captureSystemErr.during {
            clipboardService.post(outputOf(shellOf(message)))
        }

        // then
        verify(exactly = 0) { clipboardGateway.copy("", true) }
        expectThat(captureSystemErr.capture) isEqualTo expectedError
    }

    @Test
    fun `should disable native clipboard tooling when configured`() {
        val message = "write this to clipboard"
        fakeConfiguration(instance = configuration, withClipboardNativeToolingEnabled = false)

        val actual = clipboardService.post(outputOf(shellOf(message)))

        expectThat(actual.success) isEqualTo true
        verify(exactly = 1) { clipboardGateway.copy(message, false) }
    }
}
