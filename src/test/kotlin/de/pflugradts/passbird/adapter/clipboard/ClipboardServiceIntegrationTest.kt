package de.pflugradts.passbird.adapter.clipboard

import de.pflugradts.passbird.NON_HEADLESS
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import io.kotest.assertions.nondeterministic.eventually
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import kotlin.time.Duration.Companion.seconds

@Tag(NON_HEADLESS)
class ClipboardServiceIntegrationTest {

    private val configuration = mockk<Configuration>()
    private val clipboardService = ClipboardService(ClipboardGateway(), configuration)

    @Test
    fun `should copy message to clipboard`() {
        runBlocking {
            // given
            val message = "write this to clipboard"
            fakeConfiguration(
                instance = configuration,
                withClipboardResetEnabled = false,
            )

            // when
            clipboardService.post(outputOf(shellOf(message)))

            // then
            val clipboard = systemClipboard()
            eventually(2.seconds) {
                expectThat(clipboard.stringData()) isEqualTo message
            }
        }
    }

    @Test
    fun `should clear clipboard`() {
        runBlocking {
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
            val clipboard = systemClipboard()
            eventually(2.seconds) {
                expectThat(clipboard.stringData()) isEqualTo message
            }
            eventually(2.seconds) {
                expectThat(clipboard.stringData()) isEqualTo ""
            }
        }
    }

    @Test
    fun `should reset clear timer`() {
        runBlocking {
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
            val clipboard = systemClipboard()
            eventually(2.seconds) {
                expectThat(clipboard.stringData()) isEqualTo anotherMessage
            }
            eventually(2.seconds) {
                expectThat(clipboard.stringData()) isEqualTo ""
            }
        }
    }
}

fun systemClipboard(): Clipboard = Toolkit.getDefaultToolkit().systemClipboard
fun Clipboard.stringData(): Any = getData(DataFlavor.stringFlavor)
