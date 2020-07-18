package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.service.password.storage.PasswordStoreAdapterPort;
import de.pflugradts.pwman3.application.PasswordStoreAdapterPortFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.application.eventhandling.PwMan3EventRegistry;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PasswordEntryRepositoryTest {

    @Mock
    private PasswordStoreAdapterPort passwordStoreAdapterPort;
    @Mock
    private PwMan3EventRegistry pwMan3EventRegistry;
    @InjectMocks
    private PasswordEntryRepository repository;

    @Captor
    private ArgumentCaptor<Supplier<Stream<PasswordEntry>>> captor;

    @BeforeEach
    private void initMocks() {
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
        then(pwMan3EventRegistry).should().register(givenPasswordEntry1);
        then(pwMan3EventRegistry).should().register(givenPasswordEntry2);
    }

    @Test
    void shouldSync() {
        // given
        final var givenPasswordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("key1")).fake();
        final var givenPasswordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("key2")).fake();
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
        final var givenKeyBytes = Bytes.of("target");
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
        final var givenKeyBytes = Bytes.of("target");
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
        final var givenKeyBytes = Bytes.of("target");
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKeyBytes).fake();
        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withEmptyPasswordEntriesList().fake();

        // when
        repository.add(givenPasswordEntry);

        // then
        then(pwMan3EventRegistry).should().register(givenPasswordEntry);
        assertThat(repository.findAll()).isNotNull().asList().hasSize(1);
        assertThat(repository.findAll().findAny()).isNotNull().contains(givenPasswordEntry);
    }

    @Test
    void shouldDeletePasswordEntry() {
        // given
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("target")).fake();
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
                .withKeyBytes(Bytes.of("firstEntry")).fake();
        final var givenPasswordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("secondEntry")).fake();
        final var givenPasswordEntry3 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("thirdEntry")).fake();

        PasswordStoreAdapterPortFaker.faker()
                .forInstance(passwordStoreAdapterPort)
                .withThesePasswordEntries(givenPasswordEntry1, givenPasswordEntry2, givenPasswordEntry3).fake();

        // when
        final var actual = repository.findAll();

        // then
        assertThat(actual).isNotNull().asList()
                .containsExactlyInAnyOrder(givenPasswordEntry1, givenPasswordEntry2, givenPasswordEntry3);
    }

}
