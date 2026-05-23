package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class FilePasswordExchangeTest {

    private val systemOperation = mockk<SystemOperation>()
    private val runContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
    private fun setupFilePasswordExchange() = FilePasswordExchange(systemOperation, runContext)

    @Test
    fun `should handle io exception on send`() {
        // given
        fakeSystemOperation(instance = systemOperation, withIoException = true)

        // when
        val captureSystemErr = captureSystemErr()
        val actual = captureSystemErr.during {
            tryCatching { setupFilePasswordExchange().send(emptyMap()) }
        }

        // then
        expectThat(actual.success).isTrue()
        expectThat(captureSystemErr.capture) contains "Password Tree could not be exported"
    }

    @Test
    fun `should handle io exception on receive`() {
        // given
        fakeSystemOperation(instance = systemOperation, withIoException = true)

        // when
        val captureSystemErr = captureSystemErr()
        val actual = captureSystemErr.during {
            tryCatching { setupFilePasswordExchange().receive() }
        }

        // then
        expectThat(actual.success).isTrue()
        expectThat(actual.getOrNull()).isNotNull()
        expectThat(actual.getOrNull()!!.toList()).isEmpty()
        expectThat(captureSystemErr.capture) contains "Password Tree could not be imported"
    }
}
