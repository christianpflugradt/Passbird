package de.pflugradts.pwman3.domain.model.transfer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class InputTest {

    @Test
    void shouldGetCommandChar() {
        // given
        final var commandChar = 'g';
        final var data = "test";
        final var input = Input.of(Bytes.of(commandChar+data));

        // when
        final var actual = input.getCommandChar();

        // then
        assertThat(actual).isNotNull().isEqualTo(commandChar);
    }

    @Test
    void shouldGetCommandChar_ReturnZeroOnEmptyBytes() {
        // given / when / then
        assertThat(Input.empty().getCommandChar()).isEqualTo((char) 0);
    }

    @Test
    void shouldGetData() {
        final var commandChar = 'g';
        final var data = "test";
        final var input = Input.of(Bytes.of(commandChar+data));
        final var expected = Bytes.of(data);

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
            final var givenBytes = Bytes.of(new byte[]{1, 2, 3});
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

}
