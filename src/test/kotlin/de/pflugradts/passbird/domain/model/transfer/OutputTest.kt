package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.emptyOutput
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class OutputTest {
    @Nested
    inner class InstantiationTest {
        @Test
        fun `should have bytes`() {
            // given
            val givenBytes = bytesOf(byteArrayOf(1, 2, 3))
            val givenOutput = outputOf(givenBytes)

            // when
            val actual = givenOutput.bytes

            // then
            expectThat(actual) isEqualTo givenBytes
        }

        @Test
        fun `should instantiate from byte array`() {
            // given
            val givenByteArray = byteArrayOf(1, 2, 3)
            val expectedBytes = bytesOf(givenByteArray)

            // when
            val actual = outputOf(givenByteArray)

            // then
            expectThat(actual.bytes) isEqualTo expectedBytes
        }

        @Test
        fun `should instantiate empty output`() {
            // given / when / then
            expectThat(emptyOutput().bytes) isEqualTo emptyBytes()
        }
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val output1 = outputOf(bytesOf("abc"))
            val output2 = output1

            // when
            val actual = output1.equals(output2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to output with equal bytes`() {
            // given
            val bytes = bytesOf("abc")
            val sameBytes = bytesOf("abc")
            val output1 = outputOf(bytes)
            val output2 = outputOf(sameBytes)

            // when
            val actual = output1.equals(output2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to output with other bytes`() {
            // given
            val bytes = bytesOf("abc")
            val otherBytes = bytesOf("abd")
            val output1 = outputOf(bytes)
            val output2 = outputOf(otherBytes)

            // when
            val actual = output1.equals(output2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val bytes = bytesOf("abc")
            val output = outputOf(bytes)

            // when
            val actual = output.equals(bytes)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val output = outputOf(bytesOf("abc"))

            // when
            val actual = output.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
