package de.pflugradts.passbird.application.util;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.BytesComparator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BytesComparatorTest {

    private final BytesComparator comparator = new BytesComparator();

    @Nested
    class NullAndEmptyTest {

        @Test
        void shouldCompare_NullAndNull() {
            // given
            final Bytes bytes1 = null;
            final Bytes bytes2 = null;

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isZero();
            assertThat(comparator.compare(bytes2, bytes1)).isZero();
        }

        @Test
        void shouldCompare_NullAndEmpty() {
            // given
            final Bytes bytes1 = Bytes.emptyBytes();
            final Bytes bytes2 = null;

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isZero();
            assertThat(comparator.compare(bytes2, bytes1)).isZero();
        }

        @Test
        void shouldCompare_NullAndNonEmpty() {
            // given
            final Bytes bytes1 = null;
            final Bytes bytes2 = Bytes.bytesOf("1");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

        @Test
        void shouldCompare_EmptyAndNonEmpty() {
            // given
            final Bytes bytes1 = Bytes.emptyBytes();
            final Bytes bytes2 = Bytes.bytesOf("1");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
       }

    }

    @Nested
    class SymbolsAndDigitsTest {

        @Test
        void shouldCompare_SymbolAndDigit() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("!");
            final Bytes bytes2 = Bytes.bytesOf("1");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

        @Test
        void shouldCompare_SymbolAndLetter() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("!");
            final Bytes bytes2 = Bytes.bytesOf("a");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

        @Test
        void shouldCompare_DigitAndLetter() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("1");
            final Bytes bytes2 = Bytes.bytesOf("a");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

    }

    @Nested
    class LettersTest {

        @Test
        void shouldCompare_UppercaseAndLowercase() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("test");
            final Bytes bytes2 = Bytes.bytesOf("TEST");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isZero();
            assertThat(comparator.compare(bytes2, bytes1)).isZero();
        }

        @Test
        void shouldCompare_ShortAndLong() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("test");
            final Bytes bytes2 = Bytes.bytesOf("TESTING");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

        @Test
        void shouldCompare_LastCharDiffers() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("testingA");
            final Bytes bytes2 = Bytes.bytesOf("testingB");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

    }

    @Nested
    class ArbitraryStringsTest {

        @Test
        void shouldCompare_ArbitraryStrings1() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("BzlPa");
            final Bytes bytes2 = Bytes.bytesOf("NeIKii75");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

        @Test
        void shouldCompare_ArbitraryStrings2() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("Ue1e;Tv");
            final Bytes bytes2 = Bytes.bytesOf("VknA@");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

        @Test
        void shouldCompare_ArbitraryStrings3() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("(F");
            final Bytes bytes2 = Bytes.bytesOf("4");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

        @Test
        void shouldCompare_ArbitraryStrings4() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("789nGE");
            final Bytes bytes2 = Bytes.bytesOf("KjLX%)Dm");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

        @Test
        void shouldCompare_ArbitraryStrings5() {
            // given
            final Bytes bytes1 = Bytes.bytesOf("7g.");
            final Bytes bytes2 = Bytes.bytesOf("r^)HVd%^f");

            // when / then
            assertThat(comparator.compare(bytes1, bytes2)).isNegative();
            assertThat(comparator.compare(bytes2, bytes1)).isPositive();
        }

    }

}
