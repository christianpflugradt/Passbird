package de.pflugradts.passbird.domain.model.namespace;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.CAPACITY;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._1;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._2;
import static org.assertj.core.api.Assertions.assertThat;

public class NamespacesTest {

    private final Namespaces namespaces = new Namespaces();

    @BeforeEach
    void reset() {
        namespaces.reset();
    }

    @Test
    void shouldPopulate() {
        // given
        final var namespaceBytes = List.of(
            Bytes.emptyBytes(), Bytes.bytesOf("namespace1"), Bytes.emptyBytes(), Bytes.bytesOf("namespace3"),
            Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.bytesOf("namespace7"), Bytes.emptyBytes());

        // when
        namespaces.populate(namespaceBytes);
        final var actual = namespaces.all().collect(Collectors.toList());

        // then
        assertThat(actual).isNotNull().isNotEmpty().hasSize(CAPACITY);
        IntStream.of(1, 3, 7).forEach(index ->
            assertThat(actual.get(index))
                .isNotEmpty().get()
                .extracting(Namespace::getBytes).isNotNull()
                .isEqualTo(namespaceBytes.get(index)));

        IntStream.of(0, 2, 4, 5, 6, 8).forEach(index -> assertThat(actual.get(index)).isEmpty());
    }

    @Test
    void shouldPopulateOnlyOnce() {
        // given
        final var givenBytes = Bytes.bytesOf("namespace");
        final var otherBytes = Bytes.bytesOf("namespaceOthers");
        final var namespaceBytes = Collections.nCopies(9, givenBytes);

        // when
        namespaces.populate(namespaceBytes);
        namespaces.populate(Collections.nCopies(9, otherBytes));
        final var actual = namespaces.all().collect(Collectors.toList());

        // then
        assertThat(actual).isNotNull().isNotEmpty().hasSize(CAPACITY);
        actual.forEach(namespace ->
            assertThat(namespace)
                .isNotEmpty().get()
                .extracting(Namespace::getBytes)
                .isEqualTo(givenBytes));
    }

    @Test
    void shouldReturnDefaultNamespaceForDefaultSlot() {
        // given / when / then
        assertThat(namespaces.atSlot(DEFAULT)).isPresent().get().isEqualTo(Namespace.DEFAULT);
    }

    @Test
    void shouldReturnNamespaceForNonEmptySlot() {
        // given
        final var givenNamespaceBytes = Bytes.bytesOf("slot2");
        final var namespaceBytes = List.of(
            Bytes.emptyBytes(), givenNamespaceBytes, Bytes.emptyBytes(), Bytes.emptyBytes(),
            Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes());

        //when
        namespaces.populate(namespaceBytes);

        //then
        final var namespace2 = namespaces.atSlot(_2).orElse(null);
        assertThat(namespace2).isNotNull();
        assertThat(namespace2.getSlot()).isNotNull().isEqualTo(_2);
        assertThat(namespace2.getBytes()).isNotNull().isEqualTo(givenNamespaceBytes);
    }

    @Test
    void shouldReturnEmptyOptionalForEmptySlot() {
        // given
        final var namespaceBytes = List.of(
            Bytes.emptyBytes(), Bytes.bytesOf("slot2"), Bytes.emptyBytes(), Bytes.emptyBytes(),
            Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes());

        //when
        namespaces.populate(namespaceBytes);

        //then
        assertThat(namespaces.atSlot(_1)).isEmpty();
    }

    @Test
    void shouldReturnDefaultNamespaceIfNoneIsSet() {
        // given
        final var namespaceBytes = List.of(
            Bytes.emptyBytes(), Bytes.bytesOf("slot2"), Bytes.emptyBytes(), Bytes.emptyBytes(),
            Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes());

        //when
        namespaces.populate(namespaceBytes);

        //then
        assertThat(namespaces.getCurrentNamespace()).isNotNull()
            .extracting(Namespace::getSlot).isNotNull()
            .isEqualTo(DEFAULT);
    }

    @Test
    void shouldUpdateAndReturnCurrentNamespace() {
        // given
        final var namespaceBytes = List.of(
            Bytes.emptyBytes(), Bytes.bytesOf("slot2"), Bytes.emptyBytes(), Bytes.emptyBytes(),
            Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes());
        namespaces.populate(namespaceBytes);
        final var wantedCurrentNamespace = _2;

        //when
        namespaces.updateCurrentNamespace(wantedCurrentNamespace);

        //then
        assertThat(namespaces.getCurrentNamespace()).isNotNull()
            .extracting(Namespace::getSlot).isNotNull()
            .isEqualTo(wantedCurrentNamespace);
    }


    @Test
    void shouldUpdateAndReturnCurrentNamespace_DoNothingIfNamespaceIsNotDeployed() {
        // given
        final var namespaceBytes = List.of(
            Bytes.emptyBytes(), Bytes.bytesOf("slot2"), Bytes.emptyBytes(), Bytes.emptyBytes(),
            Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes(), Bytes.emptyBytes());
        namespaces.populate(namespaceBytes);
        final var wantedCurrentNamespace = _1;

        //when
        namespaces.updateCurrentNamespace(wantedCurrentNamespace);

        //then
        assertThat(namespaces.getCurrentNamespace()).isNotNull()
            .extracting(Namespace::getSlot).isNotNull()
            .isNotEqualTo(wantedCurrentNamespace)
            .isEqualTo(DEFAULT);
    }

}
