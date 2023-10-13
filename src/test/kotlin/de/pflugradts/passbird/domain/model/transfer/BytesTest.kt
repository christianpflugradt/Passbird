package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Chars.Companion.charsOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isTrue
import java.util.Collections

class BytesTest {

    @Test
    fun test() {
        val givenBytes = byteArrayOf(1, 2, 3)
        val bytes = bytesOf(givenBytes)
        println(bytes.toByteArray().toList())
        println(bytes.toMutableList().also { it.remove(1.toByte()) })
    }

    @Test
    fun `should have correct size`() {
        // given
        val givenBytes = byteArrayOf(1, 2, 3)
        val bytes = bytesOf(givenBytes)
        val expectedSize = 3

        // when
        val actual = bytes.size

        // then
        expectThat(actual) isEqualTo expectedSize
    }

    @Test
    fun `should get byte`() {
        // given
        val givenBytes = byteArrayOf(1, 2, 3)
        val bytes = bytesOf(givenBytes)

        // when / then
        expectThat(givenBytes.size) isEqualTo 3
        givenBytes.indices.forEach {
            expectThat(bytes.getByte(it)) isEqualTo givenBytes[it]
        }
    }

    @Test
    fun `should throw exception on invalid index`() {
        // given
        val givenBytes = byteArrayOf(1, 2, 3)
        val invalidIndex = 4
        val bytes = bytesOf(givenBytes)

        // when
        val exception = tryCatching { bytes.getByte(invalidIndex) }.exceptionOrNull()

        // then
        expectThat(exception).isA<ArrayIndexOutOfBoundsException>()
    }

    @Test
    fun `should return slice`() {
        // given
        val givenBytes = byteArrayOf(1, 2, 3, 4)
        val bytes = bytesOf(givenBytes)

        // when
        val actual = bytes.slice(1, 3)

        // then
        expectThat(actual).containsExactly(givenBytes[1], givenBytes[2])
    }

    @Test
    fun `should return empty bytes on negative range`() {
        // given
        val givenBytes = byteArrayOf(1, 2, 3, 4)
        val bytes = bytesOf(givenBytes)

        // when
        val actual = bytes.slice(3, 1)

        // then
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should throw exception on out of bounds range`() {
        // given
        val givenBytes = byteArrayOf(1, 2, 3)
        val invalidIndex = 4
        val bytes = bytesOf(givenBytes)

        // when
        val exception = tryCatching { bytes.slice(invalidIndex, invalidIndex + 1) }.exceptionOrNull()

        // then
        expectThat(exception).isA<ArrayIndexOutOfBoundsException>()
    }

    @Test
    fun `should scramble all bytes`() {
        // given
        val minAsciiValue = 32
        val maxAsciiValue = 126
        val size = 100
        val byteList = Collections.nCopies(size, (maxAsciiValue + 1).toString().toByte())
        val bytes = bytesOf(byteList)

        // when
        bytes.forEach {
            expectThat(it) isEqualTo (maxAsciiValue + 1).toByte()
        }
        bytes.scramble()

        // then
        expectThat(bytes.size) isEqualTo size
        bytes.forEach {
            expectThat(it) isGreaterThanOrEqualTo minAsciiValue.toByte() isLessThanOrEqualTo maxAsciiValue.toByte()
        }
    }

    @Nested
    inner class InstantiationTest {
        @Test
        fun `should instantiate from byte array`() {
            // given
            val givenBytes = byteArrayOf(1, 2, 3)
            val expectedBytes = byteArrayOf(givenBytes[0], givenBytes[1], givenBytes[2])

            // when
            val actual = bytesOf(givenBytes)

            // then
            expectThat(actual).containsExactly(*expectedBytes.toTypedArray())
        }

        @Test
        fun `should instantiate from char array`() {
            // given
            val givenChars = charArrayOf('a', 'b', 'c')
            val expectedBytes = byteArrayOf(givenChars[0].code.toByte(), givenChars[1].code.toByte(), givenChars[2].code.toByte())

            // when
            val actual = charsOf(givenChars).toBytes()

            // then
            expectThat(actual).containsExactly(*expectedBytes.toTypedArray())
        }

        @Test
        fun `should instantiate from byte list`() {
            // given
            val givenBytes = listOf("1".toByte(), "2".toByte(), "3".toByte())
            val expectedBytes = byteArrayOf(givenBytes[0], givenBytes[1], givenBytes[2])

            // when
            val actual = bytesOf(givenBytes)

            // then
            expectThat(actual).containsExactly(*expectedBytes.toTypedArray())
        }

        @Test
        fun `should instantiate from string`() {
            // given
            val givenString = "hello"
            val primitiveBytes = givenString.toByteArray()
            val expectedBytes = primitiveBytes.copyOf()

            // when
            val actual = bytesOf(givenString)

            // then
            expectThat(actual).containsExactly(*expectedBytes.toTypedArray())
        }

        @Test
        fun `should instantiate empty bytes`() {
            // given / when / then
            expectThat(emptyBytes().isEmpty).isTrue()
        }

        @Test
        fun `should clone constructor input`() {
            // given
            val givenBytes = byteArrayOf(1, 2, 3)
            val expectedBytes = byteArrayOf(1, 2, 3)

            // when
            val actual = bytesOf(givenBytes)
            givenBytes[0] = 4 // change original array

            // then
            expectThat(actual).containsExactly(*expectedBytes.toTypedArray())
            expectThat(givenBytes) isEqualTo byteArrayOf(4, 2, 3)
        }
    }

    @Nested
    inner class InstantiationFromEmptySourceTest {
        @Test
        fun `should instantiate from empty byte array`() {
            // given
            val givenBytes = byteArrayOf()
            val expectedBytes = emptyBytes()

            // when
            val actual = bytesOf(givenBytes)

            // then
            expectThat(actual).containsExactly(*expectedBytes.toByteArray().toTypedArray())
        }

        @Test
        fun `should instantiate from empty char array`() {
            // given
            val givenChars = charArrayOf()
            val expectedBytes = emptyBytes()

            // when
            val actual = charsOf(givenChars).toBytes()

            // then
            expectThat(actual).containsExactly(*expectedBytes.toByteArray().toTypedArray())
        }

        @Test
        fun `should instantiate from empty byte list`() {
            // given
            val givenBytes = emptyList<Byte>()
            val expectedBytes = emptyBytes()

            // when
            val actual = bytesOf(givenBytes)

            // then
            expectThat(actual).containsExactly(*expectedBytes.toByteArray().toTypedArray())
        }

        @Test
        fun `should instantiate from empty string`() {
            // given
            val givenString = ""
            val expectedBytes = emptyBytes()

            // when
            val actual = bytesOf(givenString)

            // then
            expectThat(actual).containsExactly(*expectedBytes.toByteArray().toTypedArray())
        }
    }

    @Nested
    inner class TransformationTest {
        @Test
        fun `should transform to byte array`() {
            // given
            val givenBytes = byteArrayOf(1, 2, 3)
            val bytes = bytesOf(givenBytes)

            // when
            val actual = bytes.toByteArray()

            // then
            expectThat(actual) isEqualTo givenBytes isNotSameInstanceAs givenBytes
        }

        @Test
        fun `should transform to char array`() {
            // given
            val givenChars = charArrayOf('a', 'b', 'c')
            val referenceChars = charArrayOf('a', 'b', 'c')
            val bytes = charsOf(givenChars).toBytes()

            // when
            val actual = bytes.toChars()

            // then
            expectThat(actual.toCharArray()) isEqualTo referenceChars
        }

        @Test
        fun `should transform to string`() {
            // given
            val givenString = "hello"
            val bytes = bytesOf(givenString)

            // when
            val actual = bytes.asString()

            // then
            expectThat(actual).isEqualTo(givenString)
        }

        @Test
        fun `should clone bytes on copy`() {
            // given
            val givenBytes = byteArrayOf(1, 2, 3)
            val bytes = bytesOf(givenBytes)

            // when
            val clonedBytes = bytes.copy()
            val actual = bytes.copy()

            // then
            expectThat(actual) isEqualTo clonedBytes isNotSameInstanceAs clonedBytes
        }

        @Test
        fun `should transform to stream`() {
            // given
            val givenBytes = bytesOf("myBytes")

            // when
            val actual = givenBytes.stream()

            // then
            expectThat(actual.toList()).containsExactly(
                'm'.code.toByte(),
                'y'.code.toByte(),
                'B'.code.toByte(),
                'y'.code.toByte(),
                't'.code.toByte(),
                'e'.code.toByte(),
                's'.code.toByte(),
            )
        }
    }

    @Nested
    inner class TransformEmptyBytesTest {
        @Test
        fun `should transform to byte array`() {
            // given
            val bytes = emptyBytes()

            // when
            val actual = bytes.toByteArray()

            // then
            expectThat(actual).isEmpty()
        }

        @Test
        fun `should transform to char array`() {
            // given
            val bytes = emptyBytes()

            // when
            val actual = bytes.toChars()

            // then
            expectThat(actual.toCharArray()) isEqualTo charArrayOf()
        }

        @Test
        fun `should transform to string`() {
            // given
            val bytes = emptyBytes()

            // when
            val actual = bytes.asString()

            // then
            expectThat(actual).isEmpty()
        }
    }

    @Nested
    inner class IteratorTest {
        @Test
        fun `should iterate over elements`() {
            // given
            val givenBytes = byteArrayOf(1, 2, 3)
            val actualBytes = ByteArray(givenBytes.size)
            val iterator = bytesOf(givenBytes).iterator()

            // when
            var index = 0
            while (iterator.hasNext()) {
                actualBytes[index++] = iterator.next()
            }

            // then
            expectThat(actualBytes) isEqualTo givenBytes
        }

        @Test
        fun `should throw NoSuchElementException on next when there is no more element`() {
            // given
            val givenBytes = byteArrayOf(1, 2, 3)
            val iterator = bytesOf(givenBytes).iterator()
            while (iterator.hasNext()) {
                iterator.next()
            }

            // when
            val exception = tryCatching { iterator.next() }.exceptionOrNull()

            // then
            expectThat(exception).isA<NoSuchElementException>()
        }
    }
}
