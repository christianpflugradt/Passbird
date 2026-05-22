package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class PlainShellTest {
    @Test
    fun `should convert to shell`() {
        // given
        val givenPlainShell = plainShellOf('a', 'b', 'c')
        val referencePlainShell = plainShellOf('a', 'b', 'c')

        // when
        val actual = givenPlainShell.toShell()

        // then
        expectThat(actual.toPlainShell()) isEqualTo referencePlainShell
    }

    @Test
    fun `should convert empty plainShell to shell`() {
        // given
        val givenPlainShell = plainShellOf()

        // when
        val actual = givenPlainShell.toShell()

        // then
        expectThat(actual) isEqualTo emptyShell()
    }

    @Test
    fun `should scramble plainShell when converting to shell`() {
        // given
        val givenCharArray = plainShellOf('a', 'b', 'c')
        val referenceCharArray = plainShellOf('a', 'b', 'c')

        // when
        expectThat(givenCharArray) isEqualTo referenceCharArray
        givenCharArray.toShell()

        // then
        expectThat(givenCharArray) isNotEqualTo referenceCharArray
    }

    @Test
    fun `should convert plainShell to char array`() {
        // given
        val givenCharArray = charArrayOf('a', 'b', 'c')

        // when
        val actual = plainShellOf(givenCharArray).toCharArray()

        // then
        expectThat(actual) isEqualTo givenCharArray
    }

    @Test
    fun `should convert empty plainShell to char array`() {
        // given
        val givenCharArray = charArrayOf()

        // when
        val actual = plainShellOf(givenCharArray).toCharArray()

        // then
        expectThat(actual) isEqualTo givenCharArray
    }

    @Test
    fun `should scramble plainShell`() {
        // given
        val givenCharArray = charArrayOf('a', 'b', 'c')
        val referenceCharArray = charArrayOf('a', 'b', 'c')

        // when
        expectThat(givenCharArray) isEqualTo referenceCharArray
        plainShellOf(givenCharArray).scramble()

        // then
        expectThat(givenCharArray) isNotEqualTo referenceCharArray
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val plainShell1 = plainShellOf(charArrayOf('a', 'b', 'c'))
            val plainShell2 = plainShell1

            // when
            val actual = plainShell1.equals(plainShell2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to plainShell with equal char array`() {
            // given
            val charArray = charArrayOf('a', 'b', 'c')
            val sameCharArray = charArrayOf('a', 'b', 'c')
            val plainShell1 = plainShellOf(charArray)
            val plainShell2 = plainShellOf(sameCharArray)

            // when
            val actual = plainShell1.equals(plainShell2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to plainShell with other char array`() {
            // given
            val charArray = charArrayOf('a', 'b', 'c')
            val otherCharArray = charArrayOf('a', 'b', 'd')
            val plainShell1 = plainShellOf(charArray)
            val plainShell2 = plainShellOf(otherCharArray)

            // when
            val actual = plainShell1.equals(plainShell2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val charArray = charArrayOf('a', 'b', 'c')
            val plainShell = plainShellOf(charArray)

            // when
            val actual = plainShell.equals(charArray)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val plainShell = plainShellOf(charArrayOf('a', 'b', 'c'))

            // when
            val actual = plainShell.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }

    private fun plainShellOf(vararg plainShell: Char) = plainShellOf(plainShell)
}
