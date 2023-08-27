package de.pflugradts.passbird.domain.model.password;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KeyTest {

    @Test
    void shouldCreateKey() {
        // given / when / then
        assertThat(Key.create(Bytes.of("key"))).isNotNull();
    }

    @Test
    void shouldViewKey() {
        // given
        final var bytes = Bytes.of("key");
        final var key = Key.create(bytes);

        // when
        final var actual = key.view();

        // then
        assertThat(actual).isNotNull().isEqualTo(bytes);
    }

    @Test
    void shouldRenameKey() {
        // given
        final var originalBytes = Bytes.of("key123");
        final var key = Key.create(originalBytes);
        final var updatedBytes = Bytes.of("keyABC");

        // when
        key.rename(updatedBytes);
        final var actual = key.view();

        // then
        assertThat(actual).isNotNull()
                .isEqualTo(updatedBytes)
                .isNotEqualTo(originalBytes);
    }

    @Test
    void shouldCloneBytes() {
        // given
        final var bytes = Bytes.of("key");
        final var key = Key.create(bytes);

        // when
        bytes.scramble();
        final var actual = key.view();

        // then
        assertThat(actual).isNotNull().isNotEqualTo(bytes);
    }

}
