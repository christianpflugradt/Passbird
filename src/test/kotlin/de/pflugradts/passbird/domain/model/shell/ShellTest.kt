package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isTrue
import java.util.Collections

class ShellTest {

    @Test
    fun `should have correct size`() {
        // given
        val givenShell = byteArrayOf(1, 2, 3)
        val shell = shellOf(givenShell)
        val expectedSize = 3

        // when
        val actual = shell.size

        // then
        expectThat(actual) isEqualTo expectedSize
    }

    @Test
    fun `should get byte`() {
        // given
        val givenShell = byteArrayOf(1, 2, 3)
        val shell = shellOf(givenShell)

        // when / then
        expectThat(givenShell.size) isEqualTo 3
        givenShell.indices.forEach {
            expectThat(shell.getByte(it)) isEqualTo givenShell[it]
        }
    }

    @Test
    fun `should throw exception on invalid index`() {
        // given
        val givenShell = byteArrayOf(1, 2, 3)
        val invalidIndex = 4
        val shell = shellOf(givenShell)

        // when
        val exception = tryCatching { shell.getByte(invalidIndex) }.exceptionOrNull()

        // then
        expectThat(exception).isA<ArrayIndexOutOfBoundsException>()
    }

    @Test
    fun `should return slice`() {
        // given
        val givenShell = byteArrayOf(1, 2, 3, 4)
        val shell = shellOf(givenShell)

        // when
        val actual = shell.slice(1, 3)

        // then
        expectThat(actual).containsExactly(givenShell[1], givenShell[2])
    }

    @Test
    fun `should return empty shell on negative range`() {
        // given
        val givenShell = byteArrayOf(1, 2, 3, 4)
        val shell = shellOf(givenShell)

        // when
        val actual = shell.slice(3, 1)

        // then
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should throw exception on out of bounds range`() {
        // given
        val givenShell = byteArrayOf(1, 2, 3)
        val invalidIndex = 4
        val shell = shellOf(givenShell)

        // when
        val exception = tryCatching { shell.slice(invalidIndex, invalidIndex + 1) }.exceptionOrNull()

        // then
        expectThat(exception).isA<ArrayIndexOutOfBoundsException>()
    }

    @Test
    fun `should scramble all shell`() {
        // given
        val minAsciiValue = 32
        val maxAsciiValue = 126
        val size = 100
        val byteList = Collections.nCopies(size, (maxAsciiValue + 1).toString().toByte())
        val shell = shellOf(byteList)

        // when
        shell.forEach {
            expectThat(it) isEqualTo (maxAsciiValue + 1).toByte()
        }
        shell.scramble()

        // then
        expectThat(shell.size) isEqualTo size
        shell.forEach {
            expectThat(it) isGreaterThanOrEqualTo minAsciiValue.toByte() isLessThanOrEqualTo maxAsciiValue.toByte()
        }
    }

    @Nested
    inner class InstantiationTest {
        @Test
        fun `should instantiate from byte array`() {
            // given
            val givenShell = byteArrayOf(1, 2, 3)
            val expectedShell = byteArrayOf(givenShell[0], givenShell[1], givenShell[2])

            // when
            val actual = shellOf(givenShell)

            // then
            expectThat(actual).containsExactly(*expectedShell.toTypedArray())
        }

        @Test
        fun `should instantiate from char array`() {
            // given
            val givenChars = charArrayOf('a', 'b', 'c')
            val expectedShell = byteArrayOf(givenChars[0].code.toByte(), givenChars[1].code.toByte(), givenChars[2].code.toByte())

            // when
            val actual = plainShellOf(givenChars).toShell()

            // then
            expectThat(actual).containsExactly(*expectedShell.toTypedArray())
        }

        @Test
        fun `should instantiate from byte list`() {
            // given
            val givenShell = listOf("1".toByte(), "2".toByte(), "3".toByte())
            val expectedShell = byteArrayOf(givenShell[0], givenShell[1], givenShell[2])

            // when
            val actual = shellOf(givenShell)

            // then
            expectThat(actual).containsExactly(*expectedShell.toTypedArray())
        }

        @Test
        fun `should instantiate from string`() {
            // given
            val givenString = "hello"
            val primitiveShell = givenString.toByteArray()
            val expectedShell = primitiveShell.copyOf()

            // when
            val actual = shellOf(givenString)

            // then
            expectThat(actual).containsExactly(*expectedShell.toTypedArray())
        }

        @Test
        fun `should instantiate empty shell`() {
            // given / when / then
            expectThat(emptyShell().isEmpty).isTrue()
        }

        @Test
        fun `should clone constructor input`() {
            // given
            val givenShell = byteArrayOf(1, 2, 3)
            val expectedShell = byteArrayOf(1, 2, 3)

            // when
            val actual = shellOf(givenShell)
            givenShell[0] = 4 // change original array

            // then
            expectThat(actual).containsExactly(*expectedShell.toTypedArray())
            expectThat(givenShell) isEqualTo byteArrayOf(4, 2, 3)
        }
    }

    @Nested
    inner class InstantiationFromEmptySourceTest {
        @Test
        fun `should instantiate from empty byte array`() {
            // given
            val givenShell = byteArrayOf()
            val expectedShell = emptyShell()

            // when
            val actual = shellOf(givenShell)

            // then
            expectThat(actual).containsExactly(*expectedShell.toByteArray().toTypedArray())
        }

        @Test
        fun `should instantiate from empty char array`() {
            // given
            val givenChars = charArrayOf()
            val expectedShell = emptyShell()

            // when
            val actual = plainShellOf(givenChars).toShell()

            // then
            expectThat(actual).containsExactly(*expectedShell.toByteArray().toTypedArray())
        }

        @Test
        fun `should instantiate from empty byte list`() {
            // given
            val givenShell = emptyList<Byte>()
            val expectedShell = emptyShell()

            // when
            val actual = shellOf(givenShell)

            // then
            expectThat(actual).containsExactly(*expectedShell.toByteArray().toTypedArray())
        }

        @Test
        fun `should instantiate from empty string`() {
            // given
            val givenString = ""
            val expectedShell = emptyShell()

            // when
            val actual = shellOf(givenString)

            // then
            expectThat(actual).containsExactly(*expectedShell.toByteArray().toTypedArray())
        }
    }

    @Nested
    inner class TransformationTest {
        @Test
        fun `should transform to byte array`() {
            // given
            val givenShell = byteArrayOf(1, 2, 3)
            val shell = shellOf(givenShell)

            // when
            val actual = shell.toByteArray()

            // then
            expectThat(actual) isEqualTo givenShell isNotSameInstanceAs givenShell
        }

        @Test
        fun `should transform to char array`() {
            // given
            val givenChars = charArrayOf('a', 'b', 'c')
            val referenceChars = charArrayOf('a', 'b', 'c')
            val shell = plainShellOf(givenChars).toShell()

            // when
            val actual = shell.toPlainShell()

            // then
            expectThat(actual.toCharArray()) isEqualTo referenceChars
        }

        @Test
        fun `should transform to string`() {
            // given
            val givenString = "hello"
            val shell = shellOf(givenString)

            // when
            val actual = shell.asString()

            // then
            expectThat(actual).isEqualTo(givenString)
        }

        @Test
        fun `should clone shell on copy`() {
            // given
            val givenShell = byteArrayOf(1, 2, 3)
            val shell = shellOf(givenShell)

            // when
            val clonedShell = shell.copy()
            val actual = shell.copy()

            // then
            expectThat(actual) isEqualTo clonedShell isNotSameInstanceAs clonedShell
        }

        @Test
        fun `should transform to stream`() {
            // given
            val givenShell = shellOf("myShell")

            // when
            val actual = givenShell.stream()

            // then
            expectThat(actual.toList()).containsExactly(
                'm'.code.toByte(),
                'y'.code.toByte(),
                'S'.code.toByte(),
                'h'.code.toByte(),
                'e'.code.toByte(),
                'l'.code.toByte(),
                'l'.code.toByte(),
            )
        }
    }

    @Nested
    inner class TransformEmptyShellTest {
        @Test
        fun `should transform to byte array`() {
            // given
            val shell = emptyShell()

            // when
            val actual = shell.toByteArray()

            // then
            expectThat(actual).isEmpty()
        }

        @Test
        fun `should transform to char array`() {
            // given
            val shell = emptyShell()

            // when
            val actual = shell.toPlainShell()

            // then
            expectThat(actual.toCharArray()) isEqualTo charArrayOf()
        }

        @Test
        fun `should transform to string`() {
            // given
            val shell = emptyShell()

            // when
            val actual = shell.asString()

            // then
            expectThat(actual).isEmpty()
        }
    }

    @Nested
    inner class IteratorTest {
        @Test
        fun `should iterate over elements`() {
            // given
            val givenShell = byteArrayOf(1, 2, 3)
            val actualShell = ByteArray(givenShell.size)
            val iterator = shellOf(givenShell).iterator()

            // when
            var index = 0
            while (iterator.hasNext()) {
                actualShell[index++] = iterator.next()
            }

            // then
            expectThat(actualShell) isEqualTo givenShell
        }

        @Test
        fun `should throw NoSuchElementException on next when there is no more element`() {
            // given
            val givenShell = byteArrayOf(1, 2, 3)
            val iterator = shellOf(givenShell).iterator()
            while (iterator.hasNext()) {
                iterator.next()
            }

            // when
            val exception = tryCatching { iterator.next() }.exceptionOrNull()

            // then
            expectThat(exception).isA<NoSuchElementException>()
        }
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val shell1 = shellOf("abc")
            val shell2 = shell1

            // when
            val actual = shell1.equals(shell2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to shell with equal string`() {
            // given
            val str = "abc"
            val sameStr = "abc"
            val shell1 = shellOf(str)
            val shell2 = shellOf(sameStr)

            // when
            val actual = shell1.equals(shell2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to shell with other string`() {
            // given
            val str = "abc"
            val otherStr = "abd"
            val shell1 = shellOf(str)
            val shell2 = shellOf(otherStr)

            // when
            val actual = shell1.equals(shell2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val str = "abc"
            val shell = shellOf(str)

            // when
            val actual = shell.equals(str)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val shell = shellOf("abc")

            // when
            val actual = shell.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
