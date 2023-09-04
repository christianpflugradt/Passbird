package de.pflugradts.passbird.domain.model.password;

import de.pflugradts.passbird.application.PasswordStoreAdapterPortFaker;
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.NamespaceServiceFake;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import de.pflugradts.passbird.domain.service.password.storage.PasswordStoreAdapterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PasswordEntryRepositoryTest {

    @Mock
    private PasswordStoreAdapterPort passwordStoreAdapterPort;
    @Mock
    private PassbirdEventRegistry passbirdEventRegistry;
    @Spy
    private final NamespaceServiceFake namespaceServiceFake = new NamespaceServiceFake();
    @InjectMocks
    private PasswordEntryRepository repository;

    @Captor
    private ArgumentCaptor<Supplier<Stream<PasswordEntry>>> captor;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldInitialize() {
        // given
        final var givenPasswordEntry1 = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var givenPasswordEntry2 = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(givenPasswordEntry1, givenPasswordEntry2).fake();

        // when
        repository.findAll();

        // then
        then(passbirdEventRegistry).should().register(givenPasswordEntry1);
        then(passbirdEventRegistry).should().register(givenPasswordEntry2);
    }

    @Test
    void shouldSync() {
        // given
        final var givenPasswordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("key1")).fake();
        final var givenPasswordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("key2")).fake();
        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(givenPasswordEntry1, givenPasswordEntry2).fake();

        // when
        repository.sync();

        // then
        then(passwordStoreAdapterPort).should().sync(captor.capture());
        assertThat(captor.getValue().get()).containsExactlyInAnyOrder(givenPasswordEntry1, givenPasswordEntry2);
    }

    @Test
    void shouldFindPasswordEntry() {
        // given
        final var givenKeyBytes = Bytes.bytesOf("target");
        final var expectedPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKeyBytes).fake();
        final var otherPasswordEntry1 = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var otherPasswordEntry2 = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(otherPasswordEntry1, expectedPasswordEntry, otherPasswordEntry2).fake();

        // when
        final var actual = repository.find(givenKeyBytes);

        // then
        assertThat(actual).isNotEmpty().contains(expectedPasswordEntry);
    }

    @Test
    void shouldFindPasswordEntry_ReturnEmptyOptionalIfNoMatchExists() {
        // given
        final var givenKeyBytes = Bytes.bytesOf("target");
        final var otherPasswordEntry1 = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var otherPasswordEntry2 = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(otherPasswordEntry1, otherPasswordEntry2).fake();

        // when
        final var actual = repository.find(givenKeyBytes);

        // then
        assertThat(actual).isNotNull().isEmpty();
    }

    @Test
    void shouldAddPasswordEntry() {
        // given
        final var givenKeyBytes = Bytes.bytesOf("target");
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKeyBytes).fake();
        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withEmptyPasswordEntriesList().fake();

        // when
        repository.add(givenPasswordEntry);

        // then
        then(passbirdEventRegistry).should().register(givenPasswordEntry);
        assertThat(repository.findAll()).isNotNull().asList().hasSize(1);
        assertThat(repository.findAll().findAny()).isNotNull().contains(givenPasswordEntry);
    }

    @Test
    void shouldDeletePasswordEntry() {
        // given
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("target")).fake();
        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(givenPasswordEntry).fake();

        // when
        assertThat(repository.findAll()).asList().hasSize(1);
        repository.delete(givenPasswordEntry);

        // then
        assertThat(repository.findAll()).isNotNull().isEmpty();
    }

    @Test
    void shouldFindAll() {
        // given
        final var givenPasswordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("firstEntry")).fake();
        final var givenPasswordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("secondEntry")).fake();
        final var givenPasswordEntry3 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("thirdEntry")).fake();

        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(givenPasswordEntry1, givenPasswordEntry2, givenPasswordEntry3).fake();

        // when
        final var actual = repository.findAll();

        // then
        assertThat(actual).isNotNull().asList()
                .containsExactlyInAnyOrder(givenPasswordEntry1, givenPasswordEntry2, givenPasswordEntry3);
    }

    @Nested
    class namespaceTest {

        @Test
        void shouldFindAllInCurrentNamespace() {
            // given
            final var activeNamespace = NamespaceSlot._2;
            final var otherNamespace = NamespaceSlot._3;
            namespaceServiceFake.deployAt(activeNamespace);
            namespaceServiceFake.deployAt(otherNamespace);
            namespaceServiceFake.updateCurrentNamespace(activeNamespace);

            final var givenPasswordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(activeNamespace)
                .withKeyBytes(Bytes.bytesOf("firstEntry")).fake();
            final var givenPasswordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(activeNamespace)
                .withKeyBytes(Bytes.bytesOf("secondEntry")).fake();
            final var otherPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(otherNamespace)
                .withKeyBytes(Bytes.bytesOf("thirdEntry")).fake();

            PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(givenPasswordEntry1, givenPasswordEntry2, otherPasswordEntry).fake();

            // when
            final var actual = repository.findAll();

            // then
            assertThat(actual).isNotNull().asList()
                .containsExactlyInAnyOrder(givenPasswordEntry1, givenPasswordEntry2);
        }

        @Test
        void shouldStoreMultiplePasswordEntriesWithIdenticalKeysInDifferentNamespaces() {
            // given
            final var keyBytes = Bytes.bytesOf("key");
            final var firstNamespace = NamespaceSlot._1;
            final var secondNamespace = NamespaceSlot._2;
            namespaceServiceFake.deployAt(firstNamespace);
            namespaceServiceFake.deployAt(secondNamespace);

            final var givenPasswordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(firstNamespace)
                .withKeyBytes(keyBytes).fake();
            final var givenPasswordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(secondNamespace)
                .withKeyBytes(keyBytes).fake();

            PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(givenPasswordEntry1, givenPasswordEntry2).fake();

            // when
            namespaceServiceFake.updateCurrentNamespace(firstNamespace);
            final var actualFirstEntry = repository.find(keyBytes);
            namespaceServiceFake.updateCurrentNamespace(secondNamespace);
            final var actualSecondEntry = repository.find(keyBytes);

            // then
            assertThat(actualFirstEntry).isPresent().get()
                .isEqualTo(givenPasswordEntry1)
                .isNotEqualTo(givenPasswordEntry2);
            assertThat(actualSecondEntry).isPresent().get()
                .isNotEqualTo(givenPasswordEntry1)
                .isEqualTo(givenPasswordEntry2);
        }

    }

}
