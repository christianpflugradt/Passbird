package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.nest.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Slot.INVALID
import de.pflugradts.passbird.domain.model.nest.Slot.N1
import de.pflugradts.passbird.domain.model.nest.Slot.N2
import de.pflugradts.passbird.domain.model.nest.Slot.N3
import de.pflugradts.passbird.domain.model.nest.Slot.N4
import de.pflugradts.passbird.domain.model.nest.Slot.N5
import de.pflugradts.passbird.domain.model.nest.Slot.N6
import de.pflugradts.passbird.domain.model.nest.Slot.N7
import de.pflugradts.passbird.domain.model.nest.Slot.N8
import de.pflugradts.passbird.domain.model.nest.Slot.N9
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.emptyInput
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class InputTest {
    @Test
    fun `should get command`() {
        // given
        val command = "g"
        val data = "foo"
        val input = inputOf(bytesOf(command + data))

        // when
        val actual = input.command

        // then
        expectThat(actual) isEqualTo bytesOf(command)
    }

    @Test
    fun `should get command and parse multi character command`() {
        // given
        val command = "n+1"
        val data = "foo"
        val input = inputOf(bytesOf(command + data))

        // when
        val actual = input.command

        // then
        expectThat(actual) isEqualTo bytesOf(command)
    }

    @Test
    fun `should get command and return zero on empty bytes`() {
        // given / when / then
        expectThat(emptyInput().command) isEqualTo emptyBytes()
    }

    @Test
    fun `should get data`() {
        val command = 'g'
        val data = "foo"
        val input = inputOf(bytesOf(command.toString() + data))
        val expected = bytesOf(data)

        // when
        val actual = input.data

        // then
        expectThat(actual) isEqualTo expected
    }

    @Test
    fun `should get data and return empty bytes on input without data`() {
        // given / when / then
        expectThat(emptyInput().data.isEmpty).isTrue()
    }

    @Test
    fun `should invalidate input`() {
        // given
        val bytes = spyk<Bytes>(bytesOf("foo"))

        // when
        inputOf(bytes).invalidate()

        // then
        verify(exactly = 1) { bytes.scramble() }
    }

    @Nested
    inner class InstantiationTest {
        @Test
        fun `should have bytes`() {
            // given
            val givenBytes = bytesOf(byteArrayOf(1, 2, 3))
            val givenInput = inputOf(givenBytes)

            // when
            val actual = givenInput.bytes

            // then
            expectThat(actual) isEqualTo givenBytes
        }

        @Test
        fun `should instantiate empty input`() {
            // given / when / then
            expectThat(emptyInput().bytes.isEmpty).isTrue()
        }
    }

    @Nested
    inner class ParseNestTest {
        @Test
        fun `should parse default nest`() {
            // given
            val givenIndex = 0
            val input = inputOf(givenIndex)

            // when
            val actual = input.extractNestSlot()

            // then
            expectThat(actual) isEqualTo DEFAULT
        }

        @Test
        fun `should parse nests`() {
            // given
            (1..9).zip(arrayOf(N1, N2, N3, N4, N5, N6, N7, N8, N9)).forEach {
                val givenIndex = it.first
                val expectedNestSlot = it.second
                val input = inputOf(givenIndex)

                // when
                val actual = input.extractNestSlot()

                // then
                expectThat(actual) isEqualTo expectedNestSlot
            }
        }

        @Test
        fun `should parse invalid nest with lower number`() {
            // given
            val givenIndex = -1
            val input = inputOf(givenIndex)

            // when
            val actual = input.extractNestSlot()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        @Test
        fun `should parse invalid nest with higher number`() {
            // given
            val givenIndex = 10
            val input = inputOf(givenIndex)

            // when
            val actual = input.extractNestSlot()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        @Test
        fun `should parse invalid nest with string`() {
            // given
            val givenIndex = "-A"
            val input = inputOf(bytesOf(givenIndex))

            // when
            val actual = input.extractNestSlot()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        private fun inputOf(index: Int) = inputOf(bytesOf(index.toString()))
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val input1 = inputOf(bytesOf("abc"))
            val input2 = input1

            // when
            val actual = input1.equals(input2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to input with equal bytes`() {
            // given
            val bytes = bytesOf("abc")
            val sameBytes = bytesOf("abc")
            val input1 = inputOf(bytes)
            val input2 = inputOf(sameBytes)

            // when
            val actual = input1.equals(input2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to input with other bytes`() {
            // given
            val bytes = bytesOf("abc")
            val otherBytes = bytesOf("abd")
            val input1 = inputOf(bytes)
            val input2 = inputOf(otherBytes)

            // when
            val actual = input1.equals(input2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val bytes = bytesOf("abc")
            val input = inputOf(bytes)

            // when
            val actual = input.equals(bytes)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val input = inputOf(bytesOf("abc"))

            // when
            val actual = input.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
