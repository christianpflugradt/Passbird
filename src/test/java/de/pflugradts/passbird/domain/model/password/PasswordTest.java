package de.pflugradts.passbird.domain.model.password;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class PasswordTest {

    @Test
    void shouldCreatePassword() {
        // given / when / then
        assertThat(Password.create(Bytes.of("password"))).isNotNull();
    }

    @Test
    void shouldUpdatePassword() {
        // given
        final var originalBytes = Bytes.of("password");
        final var password = Password.create(originalBytes);
        final var updatedBytes = Bytes.of("p4s5w0rD");

        // when
        password.update(updatedBytes);
        final var actual = password.view();

        // then
        assertThat(actual).isNotNull()
                .isEqualTo(updatedBytes)
                .isNotEqualTo(originalBytes);
    }

    @Test
    void shouldDiscardPassword() {
        // given
        final var givenBytes = mock(Bytes.class);
        given(givenBytes.copy()).willReturn(givenBytes);

        final var password = Password.create(givenBytes);

        // when
        password.discard();

        // then
        then(givenBytes).should().scramble();
    }

    @Test
    void shouldViewPassword() {
        // given
        final var bytes = Bytes.of("password");
        final var password = Password.create(bytes);

        // when
        final var actual = password.view();

        // then
        assertThat(actual).isNotNull().isEqualTo(bytes);
    }

    @Test
    void shouldCloneBytesOnCreation() {
        // given
        final var bytes = Bytes.of("password");
        final var password = Password.create(bytes);

        // when
        bytes.scramble();
        final var actual = password.view();

        // then
        assertThat(actual).isNotNull().isNotEqualTo(bytes);
    }

    @Test
    void shouldCloneBytesOnUpdate() {
        // given
        final var originalBytes = Bytes.of("password");
        final var password = Password.create(originalBytes);
        final var updatedBytes = Bytes.of("p4s5w0rD");

        // when
        password.update(updatedBytes);
        updatedBytes.scramble();
        final var actual = password.view();

        // then
        assertThat(actual).isNotNull().isNotEqualTo(updatedBytes);
    }

}
