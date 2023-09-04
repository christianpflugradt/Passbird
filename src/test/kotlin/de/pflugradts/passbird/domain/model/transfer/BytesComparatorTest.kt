package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan

internal class BytesComparatorTest {
    private val comparator = BytesComparator()

    @Test
    fun `should compare empty and non empty`() {
        // given
        val bytes1 = emptyBytes()
        val bytes2 = bytesOf("1")

        // when / then
        expectThat(comparator.compare(bytes1, bytes2)) isLessThan 0
        expectThat(comparator.compare(bytes2, bytes1)) isGreaterThan 0
    }

    @Nested
    internal inner class SymbolsAndDigitsTest {
        @Test
        fun `should compare symbol and digit`() {
            // given
            val bytes1 = bytesOf("!")
            val bytes2 = bytesOf("1")

            // when / then
            expectThat(comparator.compare(bytes1, bytes2)) isLessThan 0
            expectThat(comparator.compare(bytes2, bytes1)) isGreaterThan 0
        }

        @Test
        fun `should compare symbol and letter`() {
            // given
            val bytes1 = bytesOf("!")
            val bytes2 = bytesOf("a")

            // when / then
            expectThat(comparator.compare(bytes1, bytes2)) isLessThan 0
            expectThat(comparator.compare(bytes2, bytes1)) isGreaterThan 0
        }

        @Test
        fun `should compare digit and letter`() {
            // given
            val bytes1 = bytesOf("1")
            val bytes2 = bytesOf("a")

            // when / then
            expectThat(comparator.compare(bytes1, bytes2)) isLessThan 0
            expectThat(comparator.compare(bytes2, bytes1)) isGreaterThan 0
        }
    }

    @Nested
    internal inner class LettersTest {
        @Test
        fun `should compare uppercase and lowercase`() {
            // given
            val bytes1 = bytesOf("test")
            val bytes2 = bytesOf("TEST")

            // when / then
            expectThat(comparator.compare(bytes1, bytes2)) isEqualTo 0
            expectThat(comparator.compare(bytes2, bytes1)) isEqualTo 0
        }

        @Test
        fun `should compare short and long`() {
            // given
            val bytes1 = bytesOf("test")
            val bytes2 = bytesOf("TESTING")

            // when / then
            expectThat(comparator.compare(bytes1, bytes2)) isLessThan 0
            expectThat(comparator.compare(bytes2, bytes1)) isGreaterThan 0
        }

        @Test
        fun `should compare last char differs`() {
            // given
            val bytes1 = bytesOf("testingA")
            val bytes2 = bytesOf("testingB")

            // when / then
            expectThat(comparator.compare(bytes1, bytes2)) isLessThan 0
            expectThat(comparator.compare(bytes2, bytes1)) isGreaterThan 0
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
        val bytes1 = bytesOf(string1)
        val bytes2 = bytesOf(string2)

        // when / then
        expectThat(comparator.compare(bytes1, bytes2)) isLessThan 0
        expectThat(comparator.compare(bytes2, bytes1)) isGreaterThan 0
    }
}
