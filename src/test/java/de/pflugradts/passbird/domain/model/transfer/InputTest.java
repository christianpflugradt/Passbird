package de.pflugradts.passbird.domain.model.transfer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.INVALID;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._1;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class InputTest {

    @Test
    void shouldGetCommand() {
        // given
        final var command = "g";
        final var data = "test";
        final var input = Input.of(Bytes.bytesOf(command+data));

        // when
        final var actual = input.getCommand();

        // then
        assertThat(actual).isNotNull().isEqualTo(Bytes.bytesOf(command));
    }

    @Test
    void shouldGetCommand_ParseMultiCharacterCommand() {
        // given
        final var command = "n+1";
        final var data = "test";
        final var input = Input.of(Bytes.bytesOf(command+data));

        // when
        final var actual = input.getCommand();

        // then
        assertThat(actual).isNotNull().isEqualTo(Bytes.bytesOf(command));
    }

    @Test
    void shouldGetCommand_ReturnZeroOnEmptyBytes() {
        // given / when / then
        assertThat(Input.empty().getCommand()).isEqualTo(Bytes.emptyBytes());
    }

    @Test
    void shouldGetData() {
        final var command = 'g';
        final var data = "test";
        final var input = Input.of(Bytes.bytesOf(command+data));
        final var expected = Bytes.bytesOf(data);

        // when
        final var actual = input.getData();

        // then
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    void shouldGetData_ReturnEmptyBytesOnInputWithoutData() {
        // given / when / then
        assertThat(Input.empty().getData()).isEmpty();
    }

    @Test
    void shouldInvalidateInput() {
        // given
        final var bytes = mock(Bytes.class);

        // when
        Input.of(bytes).invalidate();

        // then
        then(bytes).should().scramble();
    }

    @Nested
    class InstantiationTest {

        @Test
        void shouldHaveBytes() {
            // given
            final var givenBytes = Bytes.bytesOf(new byte[]{1, 2, 3});
            final var givenInput = Input.of(givenBytes);

            // when
            final var actual = givenInput.getBytes();

            // then
            assertThat(actual).isNotNull().isEqualTo(givenBytes);
        }

        @Test
        void shouldInstantiateEmptyInput() {
            // given / when / then
            assertThat(Input.empty()).isNotNull()
                    .extracting(Input::getBytes).isNotNull()
                    .extracting(Bytes::isEmpty).isEqualTo(true);
        }

    }

    @Nested
    class ParseNamespeTest {

        @Test
        void shouldParseDefaultNamespace() {
            // given
            final var givenIndex = 0;
            final var input = inputOf(givenIndex);

            // when
            final var actual = input.parseNamespace();

            // then
            assertThat(actual).isNotNull().isEqualTo(DEFAULT);
        }

        @Test
        void shouldParseNamespace_1() {
            // given
            final var givenIndex = 1;
            final var input = inputOf(givenIndex);

            // when
            final var actual = input.parseNamespace();

            // then
            assertThat(actual).isNotNull().isEqualTo(_1);
        }

        @Test
        void shouldParseNamespace_9() {
            // given
            final var givenIndex = 9;
            final var input = inputOf(givenIndex);

            // when
            final var actual = input.parseNamespace();

            // then
            assertThat(actual).isNotNull().isEqualTo(_9);
        }

        @Test
        void shouldParseInvalidNamespace_LowerNumber() {
            // given
            final var givenIndex = -1;
            final var input = inputOf(givenIndex);

            // when
            final var actual = input.parseNamespace();

            // then
            assertThat(actual).isNotNull().isEqualTo(INVALID);
        }

        @Test
        void shouldParseInvalidNamespace_HigherNumber() {
            // given
            final var givenIndex = 10;
            final var input = inputOf(givenIndex);

            // when
            final var actual = input.parseNamespace();

            // then
            assertThat(actual).isNotNull().isEqualTo(INVALID);
        }

        @Test
        void shouldParseInvalidNamespace_String() {
            // given
            final var givenIndex = "-A";
            final var input = Input.of(Bytes.bytesOf(givenIndex));

            // when
            final var actual = input.parseNamespace();

            // then
            assertThat(actual).isNotNull().isEqualTo(INVALID);
        }

        private Input inputOf(final int index) {
            return Input.of(Bytes.bytesOf(String.valueOf(index)));
        }

    }

}
