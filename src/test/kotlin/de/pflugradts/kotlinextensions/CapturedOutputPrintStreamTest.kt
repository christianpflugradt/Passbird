package de.pflugradts.kotlinextensions

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemOut
import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.mockSystemInWith
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class CapturedOutputPrintStreamTest {

    @Test
    fun `should capture system err`() {
        // given
        val givenText = "Hello, World!"
        val otherText = "Should not be called"
        val captureSystemErr = captureSystemErr()

        // when
        captureSystemErr.during {
            System.err.print(givenText)
            System.out.print(otherText)
        }
        val actual = captureSystemErr.capture

        // then
        expectThat(actual) isEqualTo givenText isNotEqualTo otherText
    }

    @Test
    fun `should capture system out`() {
        // given
        val givenText = "Hello, World!"
        val otherText = "Should not be called"
        val captureSystemOut = captureSystemOut()

        // when
        captureSystemOut.during {
            System.err.print(otherText)
            System.out.print(givenText)
        }
        val actual = captureSystemOut.capture

        // then
        expectThat(actual) isEqualTo givenText isNotEqualTo otherText
    }

    @Test
    fun `should handle second capture`() {
        // given
        val capturedSystemErr = captureSystemErr()

        // when
        capturedSystemErr.during {
            System.err.print("foo")
        }
        val actual = tryCatching {
            capturedSystemErr.during {
                System.err.print("bar")
            }
        }

        // then
        expectThat(actual.failure)
        expectThat(actual.exceptionOrNull()!!).isA<IllegalStateException>()
    }

    @Test
    fun `should read from mocked system in`() {
        // given
        val givenText = "Hello, World!"

        // when
        val actual = mockSystemInWith(givenText) {
            String(System.`in`.readAllBytes())
        }

        // then
        expectThat(actual) isEqualTo givenText
    }
}
