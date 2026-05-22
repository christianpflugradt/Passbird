package de.pflugradts.passbird.application.util

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ByteArrayUtilsTest {
    @Test
    fun `should read int`() {
        // given
        val givenByteArray = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val givenOffset = 0
        val expectedInt = 16909060

        // when
        val actual = readInt(givenByteArray, givenOffset)

        // then
        expectThat(actual) isEqualTo expectedInt
    }

    @Test
    fun `should read int with offset`() {
        // given
        val givenByteArray = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val givenOffset = 3
        val expectedInt = 67438087

        // when
        val actual = readInt(givenByteArray, givenOffset)

        // then
        expectThat(actual) isEqualTo expectedInt
    }

    @Test
    fun `should read four bytes with no offset`() {
        // given
        val givenByteArray = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val givenOffset = 0
        val givenSize = 4
        val expectedBytes = byteArrayOf(1, 2, 3, 4)

        // when
        val actual = readBytes(givenByteArray, givenOffset, givenSize)

        // then
        expectThat(actual) isEqualTo expectedBytes
    }

    @Test
    fun `should read three bytes off by five`() {
        // given
        val givenByteArray = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val givenOffset = 5
        val givenSize = 3
        val expectedBytes = byteArrayOf(6, 7, 8)

        // when
        val actual = readBytes(givenByteArray, givenOffset, givenSize)

        // then
        expectThat(actual) isEqualTo expectedBytes
    }

    @Test
    fun `should copy bytes`() {
        // given
        val givenSourceByteArray = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val givenTargetByteArray = ByteArray(18)
        val offset = 7
        val size = givenSourceByteArray.size
        val expectedTargetByteArray = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0)

        // when
        val actual = copyBytes(givenSourceByteArray, givenTargetByteArray, offset, size)

        // then
        expectThat(actual) isEqualTo size
        expectThat(givenTargetByteArray) isEqualTo expectedTargetByteArray
    }

    @Test
    fun `should copy bytes with size unspecified`() {
        // given
        val givenSourceByteArray = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val givenTargetByteArray = ByteArray(18)
        val offset = 7
        val expectedTargetByteArray = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0)

        // when
        copyBytes(givenSourceByteArray, givenTargetByteArray, offset)

        // then
        expectThat(givenTargetByteArray) isEqualTo expectedTargetByteArray
    }
}
