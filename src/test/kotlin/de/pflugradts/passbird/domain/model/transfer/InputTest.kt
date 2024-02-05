package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.INVALID
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.slot.Slot.S6
import de.pflugradts.passbird.domain.model.slot.Slot.S7
import de.pflugradts.passbird.domain.model.slot.Slot.S8
import de.pflugradts.passbird.domain.model.slot.Slot.S9
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
        val input = inputOf(shellOf(command + data))

        // when
        val actual = input.command

        // then
        expectThat(actual) isEqualTo shellOf(command)
    }

    @Test
    fun `should get command and parse multi character command`() {
        // given
        val command = "n+1"
        val data = "foo"
        val input = inputOf(shellOf(command + data))

        // when
        val actual = input.command

        // then
        expectThat(actual) isEqualTo shellOf(command)
    }

    @Test
    fun `should get command and return zero on empty shell`() {
        // given / when / then
        expectThat(emptyInput().command) isEqualTo emptyShell()
    }

    @Test
    fun `should get data`() {
        val command = 'g'
        val data = "foo"
        val input = inputOf(shellOf(command.toString() + data))
        val expected = shellOf(data)

        // when
        val actual = input.data

        // then
        expectThat(actual) isEqualTo expected
    }

    @Test
    fun `should get data and return empty shell on input without data`() {
        // given / when / then
        expectThat(emptyInput().data.isEmpty).isTrue()
    }

    @Test
    fun `should invalidate input`() {
        // given
        val shell = spyk<Shell>(shellOf("foo"))

        // when
        inputOf(shell).invalidate()

        // then
        verify(exactly = 1) { shell.scramble() }
    }

    @Nested
    inner class InstantiationTest {
        @Test
        fun `should have shell`() {
            // given
            val givenShell = shellOf(byteArrayOf(1, 2, 3))
            val givenInput = inputOf(givenShell)

            // when
            val actual = givenInput.shell

            // then
            expectThat(actual) isEqualTo givenShell
        }

        @Test
        fun `should instantiate empty input`() {
            // given / when / then
            expectThat(emptyInput().shell.isEmpty).isTrue()
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
            (1..9).zip(arrayOf(S1, S2, S3, S4, S5, S6, S7, S8, S9)).forEach {
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
            val input = inputOf(shellOf(givenIndex))

            // when
            val actual = input.extractNestSlot()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        private fun inputOf(index: Int) = inputOf(shellOf(index.toString()))
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val input1 = inputOf(shellOf("abc"))
            val input2 = input1

            // when
            val actual = input1.equals(input2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to input with equal shell`() {
            // given
            val shell = shellOf("abc")
            val sameShell = shellOf("abc")
            val input1 = inputOf(shell)
            val input2 = inputOf(sameShell)

            // when
            val actual = input1.equals(input2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to input with other shell`() {
            // given
            val shell = shellOf("abc")
            val otherShell = shellOf("abd")
            val input1 = inputOf(shell)
            val input2 = inputOf(otherShell)

            // when
            val actual = input1.equals(input2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val shell = shellOf("abc")
            val input = inputOf(shell)

            // when
            val actual = input.equals(shell)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val input = inputOf(shellOf("abc"))

            // when
            val actual = input.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
