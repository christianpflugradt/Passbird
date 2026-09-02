package de.pflugradts.passbird.adapter.clipboard

import de.pflugradts.passbird.NON_HEADLESS
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import io.kotest.assertions.nondeterministic.eventually
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.time.Duration.Companion.seconds

@Tag(NON_HEADLESS)
@ResourceLock("system-clipboard")
class ClipboardServiceIntegrationTest {

    private val configuration = mockk<Configuration>()
    private val clipboardService = ClipboardService(ClipboardGateway(), configuration)

    @AfterEach
    fun clearSystemClipboard() {
        StringSelection("").let { selection -> systemClipboard().setContents(selection, selection) }
    }

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
}

fun systemClipboard(): Clipboard = Toolkit.getDefaultToolkit().systemClipboard
fun Clipboard.stringData(): Any = getData(DataFlavor.stringFlavor)
