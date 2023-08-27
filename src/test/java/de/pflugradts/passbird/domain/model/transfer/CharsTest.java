package de.pflugradts.passbird.domain.model.transfer;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CharsTest {

    @Test
    void shouldConvertToBytes() {
        // given
        final var givenChars = Chars.of('a', 'b', 'c');
        final var referenceChars = Chars.of('a', 'b', 'c');

        // when
        final var actual = givenChars.toBytes();

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.toChars()).isEqualTo(referenceChars);
    }

    @Test
    void shouldConvertEmptyCharsToBytes() {
        // given
        final var givenChars = Chars.of();

        // when
        final var actual = givenChars.toBytes();

        // then
        assertThat(actual).isNotNull().isEqualTo(Bytes.empty());
    }

    @Test
    void shouldConvertToBytes_ScrambleChars() {
        // given
        final var givenCharArray = new char[]{'a', 'b', 'c'};
        final var referenceCharArray = new char[]{'a', 'b', 'c'};
        final var chars = Chars.of(givenCharArray);

        // when
        assertThat(givenCharArray).isEqualTo(referenceCharArray);
        chars.toBytes();

        // then
        assertThat(givenCharArray).isNotEqualTo(referenceCharArray);
    }

    @Test
    void shouldConvertToCharArray() {
        // given
        final var givenCharArray = new char[]{'a', 'b', 'c'};

        // when
        final var actual = Chars.of(givenCharArray).toCharArray();

        // then
        assertThat(actual).isNotNull().isEqualTo(givenCharArray);
    }

    @Test
    void shouldConvertEmptyCharsToCharArray() {
        // given
        final var givenCharArray = new char[]{};

        // when
        final var actual = Chars.of(givenCharArray).toCharArray();

        // then
        assertThat(actual).isNotNull().isEqualTo(givenCharArray);
    }

    @Test
    void shouldScrambleChars() {
        // given
        final var givenCharArray = new char[]{'a', 'b', 'c'};
        final var referenceCharArray = new char[]{'a', 'b', 'c'};

        // when
        assertThat(givenCharArray).isEqualTo(referenceCharArray);
        Chars.scramble(givenCharArray);

        // then
        assertThat(givenCharArray).isNotEqualTo(referenceCharArray);
    }

}
