package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class FilePasswordExchangeTest {

    private val systemOperation = mockk<SystemOperation>()
    private val filePasswordExchange = FilePasswordExchange("", systemOperation)

    @Test
    fun `should handle io exception on send`() {
        // given
        fakeSystemOperation(instance = systemOperation, withIoException = true)

        // when
        val captureSystemErr = captureSystemErr()
        val actual = captureSystemErr.during {
            tryCatching { filePasswordExchange.send(emptyMap()) }
        }

        // then
        expectThat(actual.success).isTrue()
        expectThat(captureSystemErr.capture) contains "Password database could not be exported"
    }

    @Test
    fun `should handle io exception on receive`() {
        // given
        fakeSystemOperation(instance = systemOperation, withIoException = true)

        // when
        val captureSystemErr = captureSystemErr()
        val actual = captureSystemErr.during {
            tryCatching { filePasswordExchange.receive() }
        }

        // then
        expectThat(actual.success).isTrue()
        expectThat(actual.getOrNull()).isNotNull()
        expectThat(actual.getOrNull()!!.toList()).isEmpty()
        expectThat(captureSystemErr.capture) contains "Password database could not be imported"
    }
}
