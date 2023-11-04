package de.pflugradts.passbird.adapter.exchange

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
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.stream.Stream

class FilePasswordExchangeTest {

    private val systemOperation = mockk<SystemOperation>()
    private val filePasswordExchange = FilePasswordExchange("", systemOperation)

    @Test
    fun `should handle io exception on send`() {
        // given
        fakeSystemOperation(instance = systemOperation, withIoException = true)

        // when
        ByteArrayOutputStream().use { stream ->
            PrintStream(stream).use { printStream ->
                System.setErr(printStream)
                val actual = tryCatching { filePasswordExchange.send(Stream.empty()) }
                val errorOutput = String(stream.toByteArray())

                // then
                expectThat(actual.success).isTrue()
                expectThat(errorOutput) contains "Password database could not be exported"
            }
        }
    }

    @Test
    fun `should handle io exception on receive`() {
        // given
        fakeSystemOperation(instance = systemOperation, withIoException = true)

        // when
        ByteArrayOutputStream().use { stream ->
            PrintStream(stream).use { printStream ->
                System.setErr(printStream)
                val actual = tryCatching { filePasswordExchange.receive() }
                val errorOutput = String(stream.toByteArray())

                // then
                expectThat(actual.success).isTrue()
                expectThat(actual.getOrNull()).isNotNull()
                expectThat(actual.getOrNull()!!.toList()).isEmpty()
                expectThat(errorOutput) contains "Password database could not be imported"
            }
        }
    }
}
