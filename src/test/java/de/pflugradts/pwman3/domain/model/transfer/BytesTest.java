package de.pflugradts.pwman3.domain.model.transfer;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BytesTest {

    @Test
    void shouldHasCorrectSize() {
        // given
        final var givenBytes = new byte[]{1, 2, 3};
        final var bytes = Bytes.of(givenBytes);
        final var expectedSize = 3;

        // when
        final var actual = bytes.size();

        // then
        assertThat(actual).isNotNull().isEqualTo(expectedSize);
    }

    @Test
    void shouldGetByte() {
        // given
        final var givenBytes = new byte[]{1, 2, 3};
        final var bytes = Bytes.of(givenBytes);

        // when / then
        assertThat(givenBytes.length).isEqualTo(3);
        for (int i = 0; i < givenBytes.length; i++) {
            assertThat(bytes.getByte(i)).isEqualTo(givenBytes[i]);
        }
    }

    @Test
    void shouldGetByte_ThrowArrayIndexOutOfBoundsException_OnInvalidIndex() {
        // given
        final var givenBytes = new byte[]{1, 2, 3};
        final var invalidIndex = 4;
        final var bytes = Bytes.of(givenBytes);

        // when
        final Throwable throwable = catchThrowable(() -> bytes.getByte(invalidIndex));

        // then
        assertThat(throwable).isNotNull().isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    void shouldSlice() {
        // given
        final var givenBytes = new byte[]{1, 2, 3, 4};
        final var bytes = Bytes.of(givenBytes);

        // when
        final var actual = bytes.slice(1, 3);

        // then
        assertThat(actual).isNotNull().containsExactly(givenBytes[1], givenBytes[2]);
    }

    @Test
    void shouldSlice_ReturnEmptyBytes_OnInvalidRange() {
        // given
        final var givenBytes = new byte[]{1, 2, 3, 4};
        final var bytes = Bytes.of(givenBytes);

        // when
        final var actual = bytes.slice(3, 1);

        // then
        assertThat(actual).isNotNull().isEmpty();
    }

    @Test
    void shouldSlice_ThrowArrayIndexOutOfBoundsException_OnInvalidIndex() {
        // given
        final var givenBytes = new byte[]{1, 2, 3};
        final var invalidIndex = 4;
        final var bytes = Bytes.of(givenBytes);

        // when
        final Throwable throwable = catchThrowable(() -> bytes.slice(invalidIndex, invalidIndex + 1));

        // then
        assertThat(throwable).isNotNull().isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    void shouldScrambleAllBytes() {
        // given
        final var minAsciiValue = 32;
        final var maxAsciiValue = 126;
        final var size = 100;
        final var byteList = Collections.nCopies(size,
                Byte.valueOf(String.valueOf(maxAsciiValue + 1)));
        final var bytes = Bytes.of(byteList);

        // when
        bytes.forEach(b -> assertThat(b).isEqualTo((byte) (maxAsciiValue + 1)));
        bytes.scramble();

        // then
        assertThat(bytes.size()).isEqualTo(size);
        bytes.forEach(b -> assertThat(b).isBetween((byte) minAsciiValue, (byte) maxAsciiValue));
    }

    @Nested
    class InstantiationTest {

        @Test
        void shouldInstantiateFromByteArray() {
            // given
            final var givenBytes = new byte[]{1, 2, 3};
            final var expectedBytes = new Byte[]{givenBytes[0], givenBytes[1], givenBytes[2]};

            // when
            final var actual = Bytes.of(givenBytes);

            // then
            assertThat(actual).isNotNull().containsExactly(expectedBytes);
        }

        @Test
        void shouldInstantiateFromCharArray() {
            // given
            final var givenChars = new char[]{'a', 'b', 'c'};
            final var expectedBytes = new Byte[]{(byte) givenChars[0], (byte) givenChars[1], (byte) givenChars[2]};

            // when
            final var actual = Chars.of(givenChars).toBytes();

            // then
            assertThat(actual).isNotNull().containsExactly(expectedBytes);
        }

        @Test
        void shouldInstantiateFromByteList() {
            // given
            final var givenBytes = List.of(Byte.valueOf("1"), Byte.valueOf("2"), Byte.valueOf("3"));
            final var expectedBytes = new Byte[]{givenBytes.get(0), givenBytes.get(1), givenBytes.get(2)};

            // when
            final var actual = Bytes.of(givenBytes);

            // then
            assertThat(actual).isNotNull().containsExactly(expectedBytes);
        }

        @Test
        void shouldInstantiateFromString() {
            // given
            final var givenString = "hello";
            final var primitiveBytes = givenString.getBytes();
            final var expectedBytes = IntStream.range(0, primitiveBytes.length)
                    .mapToObj(i -> primitiveBytes[i])
                    .toArray(Byte[]::new);

            // when
            final var actual = Bytes.of(givenString);

            // then
            assertThat(actual).isNotNull().containsExactly(expectedBytes);
        }

        @Test
        void shouldInstantiateEmptyBytes() {
            // given / when / then
            assertThat(Bytes.empty()).isNotNull().isEmpty();
        }

        @Test
        void shouldCloneConstructorInput() {
            // given
            final var givenBytes = new byte[]{1, 2, 3};
            final var expectedBytes = new Byte[]{1, 2, 3};

            // when
            final var actual = Bytes.of(givenBytes);
            givenBytes[0] = 4; // change original array

            // then
            assertThat(actual).isNotNull().containsExactly(expectedBytes);
            assertThat(givenBytes).isEqualTo(new byte[]{4, 2, 3});
        }

    }

    @Nested
    class TransformationTest {

        @Test
        void shouldTransformToByteArray() {
            // given
            final var givenBytes = new byte[]{1, 2, 3};
            final var bytes = Bytes.of(givenBytes);

            // when
            final var actual = bytes.toByteArray();

            // then
            assertThat(actual).isNotNull()
                    .isEqualTo(givenBytes)
                    .isNotSameAs(givenBytes);
        }

        @Test
        void shouldTransformToCharArray() {
            // given
            final var givenChars = new char[]{'a', 'b', 'c'};
            final var referenceChars = new char[]{'a', 'b', 'c'};
            final var bytes = Chars.of(givenChars).toBytes();

            // when
            final var actual = bytes.toChars();

            // then
            assertThat(actual).isNotNull().extracting(Chars::toCharArray).isEqualTo(referenceChars);
        }

        @Test
        void shouldTransformToString() {
            // given
            final var givenString = "hello";
            final var bytes = Bytes.of(givenString);

            // when
            final var actual = bytes.asString();

            // then
            assertThat(actual).isNotNull().isEqualTo(givenString);
        }

        @Test
        void shouldCloneBytesOnCopy() {
            // given
            final var givenBytes = new byte[]{1, 2, 3};
            final var bytes = Bytes.of(givenBytes);

            // when
            final var clonedBytes = bytes.copy();
            final var actual = bytes.copy();

            // then
            assertThat(actual).isNotNull()
                    .isEqualTo(clonedBytes)
                    .isNotSameAs(clonedBytes);
        }

    }

    @Nested
    class IteratorTest {

        @Test
        void shouldIterateOverElements() {
            // given
            final var givenBytes = new byte[]{1, 2, 3};
            final var actualBytes = new byte[givenBytes.length];
            final var iterator = Bytes.of(givenBytes).iterator();

            // when
            var index = 0;
            while (iterator.hasNext()) {
                actualBytes[index++] = iterator.next();
            }

            // then
            assertThat(actualBytes).isEqualTo(givenBytes);
        }

        @Test
        void shouldThrowNoSuchElementException_OnNext_WhenThereIsNoMoreElement() {
            // given
            final var givenBytes = new byte[]{1, 2, 3};
            final var iterator = Bytes.of(givenBytes).iterator();
            while (iterator.hasNext()) {
                iterator.next();
            }

            // when
            final Throwable throwable = catchThrowable(iterator::next);

            // then
            assertThat(throwable).isNotNull().isInstanceOf(NoSuchElementException.class);
        }

    }

}
