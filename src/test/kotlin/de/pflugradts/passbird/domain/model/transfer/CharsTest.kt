package de.pflugradts.passbird.domain.model.transfer

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

internal class CharsTest {
    @Test
    fun `should convert to bytes`() {
        // given
        val givenChars = Chars.of('a', 'b', 'c')
        val referenceChars = Chars.of('a', 'b', 'c')

        // when
        val actual = givenChars.toBytes()

        // then
        expectThat(actual.toChars()) isEqualTo referenceChars
    }

    @Test
    fun `should convert empty chars to bytes`() {
        // given
        val givenChars = Chars.of()

        // when
        val actual = givenChars.toBytes()

        // then
        expectThat(actual) isEqualTo Bytes.empty()
    }

    @Test
    fun `should scramble chars when converting to bytes`() {
        // given
        val givenCharArray = Chars.of('a', 'b', 'c')
        val referenceCharArray = Chars.of('a', 'b', 'c')

        // when
        expectThat(givenCharArray) isEqualTo referenceCharArray
        givenCharArray.toBytes()

        // then
        expectThat(givenCharArray) isNotEqualTo referenceCharArray
    }

    @Test
    fun `should convert chars to char array`() {
        // given
        val givenCharArray = charArrayOf('a', 'b', 'c')

        // when
        val actual = Chars.of(givenCharArray).toCharArray()

        // then
        expectThat(actual) isEqualTo givenCharArray
    }

    @Test
    fun `should convert empty chars to char array`() {
        // given
        val givenCharArray = charArrayOf()

        // when
        val actual = Chars.of(givenCharArray).toCharArray()

        // then
        expectThat(actual) isEqualTo givenCharArray
    }

    @Test
    fun `should scramble chars`() {
        // given
        val givenCharArray = charArrayOf('a', 'b', 'c')
        val referenceCharArray = charArrayOf('a', 'b', 'c')

        // when
        expectThat(givenCharArray) isEqualTo referenceCharArray
        Chars.of(givenCharArray).scramble()

        // then
        expectThat(givenCharArray) isNotEqualTo referenceCharArray
    }

    fun Chars.Companion.of(vararg chars: Char) = of(chars)
}
