package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
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
