package de.pflugradts.pwman3.domain.model.namespace;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import org.junit.jupiter.api.Test;

import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;

public class NamespaceTest {

    @Test
    void shouldCreateNamespace() {
        // given / when / then
        assertThat(Namespace.create(Bytes.of("namespace"), DEFAULT)).isNotNull();
    }

    @Test
    void shouldCloneBytes() {
        // given
        final var bytes = Bytes.of("key");
        final var namespace = Namespace.create(bytes, DEFAULT);

        // when
        bytes.scramble();
        final var actual = namespace.getBytes();

        // then
        assertThat(actual).isNotNull().isNotEqualTo(bytes);
    }

    @Test
    void shouldCreateDefaultNamespace() {
        // given / when
        final var defaultNamespace = Namespace.DEFAULT;

        // then
        assertThat(defaultNamespace).isNotNull();
        assertThat(defaultNamespace.getBytes()).isNotNull().isEqualTo(Bytes.of("Default"));
        assertThat(defaultNamespace.getSlot()).isNotNull().isEqualTo(DEFAULT);
    }

}
