package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Chars.Companion.charsOf
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class CharsTest {
    @Test
    fun `should convert to bytes`() {
        // given
        val givenChars = charsOf('a', 'b', 'c')
        val referenceChars = charsOf('a', 'b', 'c')

        // when
        val actual = givenChars.toBytes()

        // then
        expectThat(actual.toChars()) isEqualTo referenceChars
    }

    @Test
    fun `should convert empty chars to bytes`() {
        // given
        val givenChars = charsOf()

        // when
        val actual = givenChars.toBytes()

        // then
        expectThat(actual) isEqualTo emptyBytes()
    }

    @Test
    fun `should scramble chars when converting to bytes`() {
        // given
        val givenCharArray = charsOf('a', 'b', 'c')
        val referenceCharArray = charsOf('a', 'b', 'c')

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
        val actual = charsOf(givenCharArray).toCharArray()

        // then
        expectThat(actual) isEqualTo givenCharArray
    }

    @Test
    fun `should convert empty chars to char array`() {
        // given
        val givenCharArray = charArrayOf()

        // when
        val actual = charsOf(givenCharArray).toCharArray()

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
        charsOf(givenCharArray).scramble()

        // then
        expectThat(givenCharArray) isNotEqualTo referenceCharArray
    }

    private fun charsOf(vararg chars: Char) = charsOf(chars)
}
