package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.INVALID
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._1
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._2
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._3
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._4
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._5
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._6
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._7
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._8
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._9
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
import strikt.assertions.isTrue

internal class InputTest {
    @Test
    fun shouldGetCommand() {
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
    fun shouldGetCommand_ParseMultiCharacterCommand() {
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
    fun shouldGetCommand_ReturnZeroOnEmptyBytes() {
        // given / when / then
        expectThat(emptyInput().command) isEqualTo emptyBytes()
    }

    @Test
    fun shouldGetData() {
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
    fun shouldGetData_ReturnEmptyBytesOnInputWithoutData() {
        // given / when / then
        expectThat(emptyInput().data.isEmpty).isTrue()
    }

    @Test
    fun shouldInvalidateInput() {
        // given
        val bytes = spyk<Bytes>(bytesOf("foo"))

        // when
        inputOf(bytes).invalidate()

        // then
        verify(exactly = 1) { bytes.scramble() }
    }

    @Nested
    internal inner class InstantiationTest {
        @Test
        fun shouldHaveBytes() {
            // given
            val givenBytes = bytesOf(byteArrayOf(1, 2, 3))
            val givenInput = inputOf(givenBytes)

            // when
            val actual = givenInput.bytes

            // then
            expectThat(actual) isEqualTo givenBytes
        }

        @Test
        fun shouldInstantiateEmptyInput() {
            // given / when / then
            expectThat(emptyInput().bytes.isEmpty).isTrue()
        }
    }

    @Nested
    internal inner class ParseNamespeTest {
        @Test
        fun shouldParseDefaultNamespace() {
            // given
            val givenIndex = 0
            val input = inputOf(givenIndex)

            // when
            val actual = input.parseNamespace()

            // then
            expectThat(actual) isEqualTo DEFAULT
        }

        @Test
        fun shouldParseNamespaces() {
            // given
            (1..9).zip(arrayOf(_1, _2, _3, _4, _5, _6, _7, _8, _9)).forEach {
                val givenIndex = it.first
                val expectedNamespaceSlot = it.second
                val input = inputOf(givenIndex)

                // when
                val actual = input.parseNamespace()

                // then
                expectThat(actual) isEqualTo expectedNamespaceSlot
            }
        }

        @Test
        fun shouldParseInvalidNamespace_LowerNumber() {
            // given
            val givenIndex = -1
            val input = inputOf(givenIndex)

            // when
            val actual = input.parseNamespace()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        @Test
        fun shouldParseInvalidNamespace_HigherNumber() {
            // given
            val givenIndex = 10
            val input = inputOf(givenIndex)

            // when
            val actual = input.parseNamespace()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        @Test
        fun shouldParseInvalidNamespace_String() {
            // given
            val givenIndex = "-A"
            val input = inputOf(bytesOf(givenIndex))

            // when
            val actual = input.parseNamespace()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        private fun inputOf(index: Int) = inputOf(bytesOf(index.toString()))
    }
}
