package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Chars.Companion.charsOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

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

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val chars1 = charsOf(charArrayOf('a', 'b', 'c'))
            val chars2 = chars1

            // when
            val actual = chars1.equals(chars2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to chars with equal char array`() {
            // given
            val charArray = charArrayOf('a', 'b', 'c')
            val sameCharArray = charArrayOf('a', 'b', 'c')
            val chars1 = charsOf(charArray)
            val chars2 = charsOf(sameCharArray)

            // when
            val actual = chars1.equals(chars2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to chars with other char array`() {
            // given
            val charArray = charArrayOf('a', 'b', 'c')
            val otherCharArray = charArrayOf('a', 'b', 'd')
            val chars1 = charsOf(charArray)
            val chars2 = charsOf(otherCharArray)

            // when
            val actual = chars1.equals(chars2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val charArray = charArrayOf('a', 'b', 'c')
            val chars = charsOf(charArray)

            // when
            val actual = chars.equals(charArray)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val chars = charsOf(charArrayOf('a', 'b', 'c'))

            // when
            val actual = chars.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }

    private fun charsOf(vararg chars: Char) = charsOf(chars)
}
