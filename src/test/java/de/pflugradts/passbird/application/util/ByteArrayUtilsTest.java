package de.pflugradts.passbird.application.util;

import org.junit.jupiter.api.Test;

import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.copyBytes;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.readBytes;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.readInt;
import static org.assertj.core.api.Assertions.assertThat;

class ByteArrayUtilsTest {

    @Test
    void shouldReadInt() {
        // given
        final var givenByteArray = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        final var givenOffset = 0;
        final var expectedInt = 16909060;

        // when
        final var actual = readInt(givenByteArray, givenOffset);

        // then
        assertThat(actual).isEqualTo(expectedInt);
    }

    @Test
    void shouldReadInt_WithOffset() {
        // given
        final var givenByteArray = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        final var givenOffset = 3;
        final var expectedInt = 67438087;

        // when
        final var actual = readInt(givenByteArray, givenOffset);

        // then
        assertThat(actual).isEqualTo(expectedInt);
    }

    @Test
    void shouldReadBytes_4BytesNoOffset() {
        // given
        final var givenByteArray = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        final var givenOffset = 0;
        final var givenSize = 4;
        final var expectedBytes = new byte[]{1, 2, 3, 4};

        // when
        final var actual = readBytes(givenByteArray, givenOffset, givenSize);

        // then
        assertThat(actual).isEqualTo(expectedBytes);
    }

    @Test
    void shouldReadBytes_3BytesOffBy5() {
        // given
        final var givenByteArray = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        final var givenOffset = 5;
        final var givenSize = 3;
        final var expectedBytes = new byte[]{6, 7, 8};

        // when
        final var actual = readBytes(givenByteArray, givenOffset, givenSize);

        // then
        assertThat(actual).isEqualTo(expectedBytes);
    }

    @Test
    void shouldCopyBytes() {
        // given
        final var givenSourceByteArray = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        final var givenTargetByteArray = new byte[18];
        final var offset = 7;
        final var size = givenSourceByteArray.length;
        final var expectedTargetByteArray = new byte[]{0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0};

        // when
        final var actual = copyBytes(givenSourceByteArray, givenTargetByteArray, offset, size);

        // then
        assertThat(actual).isEqualTo(size);
        assertThat(givenTargetByteArray).isEqualTo(expectedTargetByteArray);
    }

}
