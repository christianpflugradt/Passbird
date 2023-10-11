package de.pflugradts.passbird.domain.service;

import de.pflugradts.passbird.domain.model.namespace.Namespace;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.CAPACITY;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N1;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N2;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N3;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N4;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N5;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N6;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N7;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N8;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N9;
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
        final var slot1 = namespaceService.atSlot(N1);
        final var slot4 = namespaceService.atSlot(N4);
        final var slot9 = namespaceService.atSlot(N9);

        // then
        then(passwordEntryRepository).should().requestInitialization();
        assertThat(slot1).isEmpty();
        assertThat(slot4).isEmpty();
        assertThat(slot9).isEmpty();
    }

    @Test
    void shouldHandleCallToDeployIfNotPopulated() {
        // given
        final var givenBytes = Bytes.bytesOf("test");
        final var givenSlot = N4;

        // when
        namespaceService.deploy(givenBytes, givenSlot);

        // then
        then(passwordEntryRepository).should().requestInitialization();
        assertThat(namespaceService.atSlot(givenSlot))
            .isNotEmpty().get()
            .extracting(Namespace::getBytes).isNotNull()
            .isEqualTo(givenBytes);
        assertSlotsAreEmpty(namespaceService, N1, N2, N3, N5, N6, N7, N8, N9);
    }

    @Test
    void shouldSyncPasswordStoreOnDeploy() {
        // given / when
        namespaceService.deploy(Bytes.bytesOf("test"), N4);

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
