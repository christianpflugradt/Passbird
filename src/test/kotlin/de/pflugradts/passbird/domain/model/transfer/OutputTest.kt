package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.emptyOutput
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class OutputTest {
    @Nested
    internal inner class InstantiationTest {
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
}
