package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.namespace.Namespace;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.CAPACITY;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._1;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._2;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._3;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._4;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._5;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._6;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._7;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._8;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class NamespaceServiceTest {

    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @InjectMocks
    private NamespaceService namespaceService;

    @Test
    void shouldPopulateEmpty() {
        // given / when
        namespaceService.populateEmpty();
        final var actual = namespaceService.all().collect(Collectors.toList());

        // then
        assertIsPopulatedEmpty(actual);
    }

    @Test
    void shouldHandleCallToAllIfNotPopulated() {
        // given / when
        final var actual = namespaceService.all().collect(Collectors.toList());

        // then
        then(passwordEntryRepository).should().requestInitialization();
        assertIsPopulatedEmpty(actual);
    }

    @Test
    void shouldHandleCallToAtSlotIfNotPopulated() {
        // given / when
        final var slot1 = namespaceService.atSlot(_1);
        final var slot4 = namespaceService.atSlot(_4);
        final var slot9 = namespaceService.atSlot(_9);

        // then
        then(passwordEntryRepository).should().requestInitialization();
        assertThat(slot1).isEmpty();
        assertThat(slot4).isEmpty();
        assertThat(slot9).isEmpty();
    }

    @Test
    void shouldHandleCallToDeployIfNotPopulated() {
        // given
        final var givenBytes = Bytes.of("test");
        final var givenSlot = _4;

        // when
        namespaceService.deploy(givenBytes, givenSlot);

        // then
        then(passwordEntryRepository).should().requestInitialization();
        assertThat(namespaceService.atSlot(givenSlot))
            .isNotEmpty().get()
            .extracting(Namespace::getBytes).isNotNull()
            .isEqualTo(givenBytes);
        assertSlotsAreEmpty(namespaceService, _1, _2, _3, _5, _6, _7, _8, _9);
    }

    @Test
    void shouldSyncPasswordStoreOnDeploy() {
        // given / when
        namespaceService.deploy(Bytes.of("test"), _4);

        // then
        then(passwordEntryRepository).should().sync();
    }

    private static void assertIsPopulatedEmpty(final List<Optional<Namespace>> namespaceOptionals) {
        assertThat(namespaceOptionals).isNotNull().isNotEmpty().hasSize(CAPACITY);
        namespaceOptionals.forEach(namespace -> assertThat(namespace).isEmpty());
    }

    private static void assertSlotsAreEmpty(final NamespaceService namespaceService, final NamespaceSlot... slots) {
        Arrays.stream(slots).forEach(slot -> assertThat(namespaceService.atSlot(slot)).isEmpty());
    }

}
