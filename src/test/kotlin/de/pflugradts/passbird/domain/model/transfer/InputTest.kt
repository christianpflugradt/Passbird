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
    internal inner class InstantiationTest {
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
    internal inner class ParseNamespeTest {
        @Test
        fun `should parse default namespace`() {
            // given
            val givenIndex = 0
            val input = inputOf(givenIndex)

            // when
            val actual = input.parseNamespace()

            // then
            expectThat(actual) isEqualTo DEFAULT
        }

        @Test
        fun `should parse namespaces`() {
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
        fun `should parse invalid namespace with lower number`() {
            // given
            val givenIndex = -1
            val input = inputOf(givenIndex)

            // when
            val actual = input.parseNamespace()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        @Test
        fun `should parse invalid namespace with higher number`() {
            // given
            val givenIndex = 10
            val input = inputOf(givenIndex)

            // when
            val actual = input.parseNamespace()

            // then
            expectThat(actual) isEqualTo INVALID
        }

        @Test
        fun `should parse invalid namespace with string`() {
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
