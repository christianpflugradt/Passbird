package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.application.security.CryptoProvider;
import de.pflugradts.pwman3.application.security.CryptoProviderFaker;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryRepositoryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PasswordRepositoryServiceTest {

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private CryptoProvider cryptoProvider;
    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @Mock
    private DomainEventRegistry domainEventRegistry;
    @InjectMocks
    private PasswordRepositoryService passwordService;

    @Test
    void shouldFindExistingPassword() {
        // given
        final var givenKey = Bytes.of("Key");
        final var expectedPassword = Bytes.of("Password");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKey)
                .withPasswordBytes(expectedPassword).fake();
        CryptoProviderFaker.faker().forInstance(cryptoProvider).fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        final var actual = passwordService.viewPassword(givenKey);

        // then
        then(cryptoProvider).should().encrypt(givenKey);
        then(cryptoProvider).should().decrypt(expectedPassword);
        then(domainEventRegistry).shouldHaveNoInteractions();
        assertThat(actual).isNotEmpty().contains(expectedPassword);
    }

    @Test
    void shouldFindExisting_ReturnEmptyOptionalOnNoMatch() {
        // given
        final var givenKey = Bytes.of("Key");
        final var otherKey = Bytes.of("try this");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKey).fake();
        CryptoProviderFaker.faker().forInstance(cryptoProvider).fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        final var actual = passwordService.viewPassword(otherKey);

        // then
        then(cryptoProvider).should().encrypt(otherKey);
        then(domainEventRegistry).should().register(eq(new PasswordEntryNotFound(otherKey)));
        then(domainEventRegistry).should().processEvents();
        assertThat(actual).isEmpty();
    }

    @Test
    void shouldPutPasswordEntry_InsertNewEntry() {
        // given
        final var existingKey = Bytes.of("Key");
        final var newKey = Bytes.of("try this");
        final var newPassword = Bytes.of("Password");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(existingKey).fake();
        CryptoProviderFaker.faker().forInstance(cryptoProvider).fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        passwordService.putPasswordEntry(newKey, newPassword);

        // then
        then(cryptoProvider).should().encrypt(newKey);
        then(cryptoProvider).should().encrypt(newPassword);
        then(passwordEntryRepository).should().sync();
        then(passwordEntryRepository).should().add(eq(PasswordEntry.create(newKey, newPassword)));
        then(domainEventRegistry).should().processEvents();
    }

    @Test
    void shouldPutPasswordEntry_UpdateExistingEntry() {
        // given
        final var existingKey = Bytes.of("Key");
        final var newPassword = Bytes.of("Password");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(existingKey).fake();
        CryptoProviderFaker.faker().forInstance(cryptoProvider).fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        passwordService.putPasswordEntry(existingKey, newPassword);

        // then
        then(cryptoProvider).should().encrypt(existingKey);
        then(cryptoProvider).should().encrypt(newPassword);
        then(passwordEntryRepository).should().sync();
        then(domainEventRegistry).should().processEvents();
        final var passwordEntry = passwordEntryRepository.find(existingKey);
        assertThat(passwordEntry)
                .isNotEmpty().get()
                .extracting(PasswordEntry::viewPassword).isNotNull()
                .isEqualTo(newPassword);
    }

    @Test
    void shouldDiscardPasswordEntry() {
        // given
        final var givenKey = Bytes.of("Key");
        final var givenPassword = Bytes.of("Password");
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKey)
                .withPasswordBytes(givenPassword).fake();
        CryptoProviderFaker.faker().forInstance(cryptoProvider).fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(givenPasswordEntry).fake();

        // when
        assertThat(givenPasswordEntry.viewPassword()).isEqualTo(givenPassword);
        passwordService.discardPasswordEntry(givenKey);

        // then
        then(cryptoProvider).should().encrypt(givenKey);
        then(domainEventRegistry).should().processEvents();
        assertThat(givenPasswordEntry.viewPassword()).isNotNull().isNotEqualTo(givenPassword);
    }

    @Test
    void shouldDiscardPasswordEntry_NoMatch() {
        // given
        final var givenKey = Bytes.of("Key");
        final var otherKey = Bytes.of("try this");
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKey).fake();
        CryptoProviderFaker.faker().forInstance(cryptoProvider).fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(givenPasswordEntry).fake();

        // when
        passwordService.discardPasswordEntry(otherKey);

        // then
        then(cryptoProvider).should().encrypt(otherKey);
        then(domainEventRegistry).should().register(eq(new PasswordEntryNotFound(otherKey)));
        then(domainEventRegistry).should().processEvents();
    }

    @Test
    void shouldFindAllKeysAlphabetically() {
        // given
        final var key1 = Bytes.of("abc");
        final var passwordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(key1).fake();
        final var key2 = Bytes.of("xyz");
        final var passwordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(key2).fake();
        final var key3 = Bytes.of("hij");
        final var passwordEntry3 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(key3).fake();
        CryptoProviderFaker.faker().forInstance(cryptoProvider).fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(passwordEntry1, passwordEntry2, passwordEntry3).fake();

        // when
        final var actual = passwordService.findAllKeys();

        // then
        assertThat(actual).containsExactly(key1, key3, key2);
    }

}
