package de.pflugradts.pwman3.domain.model.namespace;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.CAPACITY;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.DEFAULT;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._1;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._2;
import static org.assertj.core.api.Assertions.assertThat;

public class NamespacesTest {

    @BeforeEach
    void reset() {
        Namespaces.reset();
    }

    @Test
    void shouldPopulateEmpty() {
        // given / when
        Namespaces.populateEmpty();
        final var actual = Namespaces.all().collect(Collectors.toList());

        // then
        assertThat(actual).isNotNull().isNotEmpty().hasSize(CAPACITY);
        actual.forEach(namespace -> assertThat(namespace).isEmpty());
    }

    @Test
    void shouldPopulate() {
        // given
        final var namespaceBytes = List.of(
            Bytes.empty(), Bytes.of("namespace1"), Bytes.empty(), Bytes.of("namespace3"),
            Bytes.empty(), Bytes.empty(), Bytes.empty(), Bytes.of("namespace7"), Bytes.empty());

        // when
        Namespaces.populate(namespaceBytes);
        final var actual = Namespaces.all().collect(Collectors.toList());

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
        final var givenBytes = Bytes.of("namespace");
        final var namespaceBytes = Collections.nCopies(9, givenBytes);

        // when
        Namespaces.populate(namespaceBytes);
        Namespaces.populateEmpty();
        final var actual = Namespaces.all().collect(Collectors.toList());

        // then
        assertThat(actual).isNotNull().isNotEmpty().hasSize(CAPACITY);
        actual.forEach(namespace ->
            assertThat(namespace)
                .isNotEmpty().get()
                .extracting(Namespace::getBytes)
                .isEqualTo(givenBytes));
    }

    @Test
    void shouldReturnAllAsEmptyIfNotPopulated() {
        // given / when
        final var actual = Namespaces.all().collect(Collectors.toList());

        // then
        assertThat(actual).isNotNull().isNotEmpty().hasSize(CAPACITY);
        actual.forEach(namespace -> assertThat(namespace).isEmpty());
    }

    @Test
    void shouldReturnDefaultNamespaceForDefaultSlot() {
        // given / when / then
        assertThat(Namespaces.atSlot(DEFAULT)).isPresent().get().isEqualTo(Namespace.DEFAULT);
    }

    @Test
    void shouldReturnNamespaceForNonEmptySlot() {
        // given
        final var givenNamespaceBytes = Bytes.of("slot2");
        final var namespaceBytes = List.of(
            Bytes.empty(), givenNamespaceBytes, Bytes.empty(), Bytes.empty(),
            Bytes.empty(), Bytes.empty(), Bytes.empty(), Bytes.empty(), Bytes.empty());

        //when
        Namespaces.populate(namespaceBytes);

        //then
        final var namespace2 = Namespaces.atSlot(_2).orElse(null);
        assertThat(namespace2).isNotNull();
        assertThat(namespace2.getSlot()).isNotNull().isEqualTo(_2);
        assertThat(namespace2.getBytes()).isNotNull().isEqualTo(givenNamespaceBytes);
    }

    @Test
    void shouldReturnEmptyOptionalForEmptySlot() {
        // given
        final var namespaceBytes = List.of(
            Bytes.empty(), Bytes.of("slot2"), Bytes.empty(), Bytes.empty(),
            Bytes.empty(), Bytes.empty(), Bytes.empty(), Bytes.empty(), Bytes.empty());

        //when
        Namespaces.populate(namespaceBytes);

        //then
        assertThat(Namespaces.atSlot(_1)).isEmpty();
    }

}
