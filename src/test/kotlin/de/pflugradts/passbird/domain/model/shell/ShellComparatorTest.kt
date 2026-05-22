package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan

class ShellComparatorTest {
    private val comparator = ShellComparator()

    @Test
    fun `should compare instance to itself`() {
        // given
        val shell = shellOf("1")

        // when / then
        expectThat(comparator.compare(shell, shell)) isEqualTo 0
    }

    @Test
    fun `should compare empty and non empty`() {
        // given
        val shell1 = emptyShell()
        val shell2 = shellOf("1")

        // when / then
        expectThat(comparator.compare(shell1, shell2)) isLessThan 0
        expectThat(comparator.compare(shell2, shell1)) isGreaterThan 0
    }

    @Nested
    inner class SymbolsAndDigitsTest {
        @Test
        fun `should compare symbol and digit`() {
            // given
            val shell1 = shellOf("!")
            val shell2 = shellOf("1")

            // when / then
            expectThat(comparator.compare(shell1, shell2)) isLessThan 0
            expectThat(comparator.compare(shell2, shell1)) isGreaterThan 0
        }

        @Test
        fun `should compare symbol and letter`() {
            // given
            val shell1 = shellOf("!")
            val shell2 = shellOf("a")

            // when / then
            expectThat(comparator.compare(shell1, shell2)) isLessThan 0
            expectThat(comparator.compare(shell2, shell1)) isGreaterThan 0
        }

        @Test
        fun `should compare digit and letter`() {
            // given
            val shell1 = shellOf("1")
            val shell2 = shellOf("a")

            // when / then
            expectThat(comparator.compare(shell1, shell2)) isLessThan 0
            expectThat(comparator.compare(shell2, shell1)) isGreaterThan 0
        }
    }

    @Nested
    inner class LettersTest {
        @Test
        fun `should compare uppercase and lowercase`() {
            // given
            val shell1 = shellOf("foo")
            val shell2 = shellOf("FOO")

            // when / then
            expectThat(comparator.compare(shell1, shell2)) isEqualTo 0
            expectThat(comparator.compare(shell2, shell1)) isEqualTo 0
        }

        @Test
        fun `should compare short and long`() {
            // given
            val shell1 = shellOf("foo")
            val shell2 = shellOf("FOOBAR")

            // when / then
            expectThat(comparator.compare(shell1, shell2)) isLessThan 0
            expectThat(comparator.compare(shell2, shell1)) isGreaterThan 0
        }

        @Test
        fun `should compare last char differs`() {
            // given
            val shell1 = shellOf("fooA")
            val shell2 = shellOf("fooB")

            // when / then
            expectThat(comparator.compare(shell1, shell2)) isLessThan 0
            expectThat(comparator.compare(shell2, shell1)) isGreaterThan 0
        }
    }

    @ParameterizedTest
    @CsvSource(
        "BzlPa,NeIKii75",
        "Ue1e;Tv,VknA@",
        "(F,4",
        "789nGE,KjLX%)Dm",
        "7g.,r^)HVd%^f",
    )
    fun `should compare arbitrary strings`(string1: String, string2: String) {
        // given
        val shell1 = shellOf(string1)
        val shell2 = shellOf(string2)

        // when / then
        expectThat(comparator.compare(shell1, shell2)) isLessThan 0
        expectThat(comparator.compare(shell2, shell1)) isGreaterThan 0
    }
}
