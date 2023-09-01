package de.pflugradts.passbird.adapter.clipboard

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Output
import io.mockk.mockk
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.util.concurrent.TimeUnit

@Disabled
internal class ClipboardServiceIT {

    private val systemOperation = SystemOperation()
    private val configuration = mockk<Configuration>()
    private val clipboardService = ClipboardService(systemOperation, configuration)

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
        val clipboard = systemClipboard()
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            expectThat(clipboard.stringData()) isEqualTo message
        }
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
        val clipboard = systemClipboard()
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            expectThat(clipboard.stringData()) isEqualTo message
        }
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            expectThat(clipboard.stringData()) isEqualTo ""
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
        val clipboard = systemClipboard()
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            expectThat(clipboard.stringData()) isEqualTo anotherMessage
        }
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            expectThat(clipboard.stringData()) isEqualTo ""
        }
    }
}

fun systemClipboard(): Clipboard = Toolkit.getDefaultToolkit().systemClipboard
fun Clipboard.stringData(): Any = getData(DataFlavor.stringFlavor)
