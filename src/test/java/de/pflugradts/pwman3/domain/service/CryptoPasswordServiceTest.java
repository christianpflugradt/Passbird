package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.application.eventhandling.PwMan3EventRegistry;
import de.pflugradts.pwman3.application.security.CryptoProviderFaker;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.password.InvalidKeyException;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryRepositoryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.CryptoPasswordService;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.Tuple2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CryptoPasswordServiceTest {

    @Mock
    private CryptoProvider cryptoProvider;
    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @Mock
    private PwMan3EventRegistry pwMan3EventRegistry;
    @InjectMocks
    private CryptoPasswordService passwordService;

    @Test
    void shouldReturnTrue_IfEntryExists() {
        // given
        final var givenKey = Bytes.of("Key");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKey).fake();
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        final var actual = passwordService.entryExists(givenKey);

        // then
        then(pwMan3EventRegistry).shouldHaveNoInteractions();
        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isTrue();
    }

    @Test
    void shouldReturnFalse_IfEntryDoesNotExist() {
        // given
        final var givenKey = Bytes.of("Key");
        final var otherKey = Bytes.of("try this");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKey).fake();
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        final var actual = passwordService.entryExists(otherKey);

        // then
        then(pwMan3EventRegistry).shouldHaveNoInteractions();
        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isFalse();
    }

    @Test
    void shouldFindExistingPassword() {
        // given
        final var givenKey = Bytes.of("Key");
        final var expectedPassword = Bytes.of("Password");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKey)
                .withPasswordBytes(expectedPassword).fake();
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        final var actual = passwordService.viewPassword(givenKey);

        // then
        then(cryptoProvider).should().encrypt(givenKey);
        then(cryptoProvider).should().decrypt(expectedPassword);
        then(pwMan3EventRegistry).shouldHaveNoInteractions();
        assertThat(actual).isNotEmpty();
        assertThat(actual.get().isSuccess()).isTrue();
        assertThat(actual.get()).contains(expectedPassword);
    }

    @Test
    void shouldFindExistingPassword_ReturnEmptyOptionalOnNoMatch() {
        // given
        final var givenKey = Bytes.of("Key");
        final var otherKey = Bytes.of("tryThis");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenKey).fake();
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        final var actual = passwordService.viewPassword(otherKey);

        // then
        then(cryptoProvider).should().encrypt(otherKey);
        then(pwMan3EventRegistry).should().register(eq(new PasswordEntryNotFound(otherKey)));
        then(pwMan3EventRegistry).should().processEvents();
        assertThat(actual).isEmpty();
    }

    @Test
    void shouldSucceedChallengingAlphabeticAlias() {
        // given
        final var givenAlias = Bytes.of("abcDEF");

        // when
        final var actual = passwordService.challengeAlias(givenAlias);

        // then
        assertThat(actual.isSuccess()).isTrue();
    }

    @Test
    void shouldSucceedChallengingAliasWithDigitAtOtherThanFirstPosition() {
        // given
        final var givenAlias = Bytes.of("abc123");

        // when
        final var actual = passwordService.challengeAlias(givenAlias);

        // then
        assertThat(actual.isSuccess()).isTrue();
    }

    @Test
    void shouldFailChallengingAliasWithDigitAtFirstPosition() {
        // given
        final var givenAlias = Bytes.of("123abc");

        // when
        final var actual = passwordService.challengeAlias(givenAlias);

        // then
        assertThat(actual.isSuccess()).isFalse();
    }


    @Test
    void shouldFailChallengingAliasWithSpecialCharacters() {
        // given
        final var givenAlias = Bytes.of("abc!");

        // when
        final var actual = passwordService.challengeAlias(givenAlias);

        // then
        assertThat(actual.isSuccess()).isFalse();
    }

    @Test
    void shouldPutPasswordEntry_InsertNewEntry() {
        // given
        final var existingKey = Bytes.of("Key");
        final var newKey = Bytes.of("tryThis");
        final var newPassword = Bytes.of("Password");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(existingKey).fake();
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
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
        then(pwMan3EventRegistry).should().processEvents();
    }

    @Test
    void shouldPutPasswordEntry_UpdateExistingEntry() {
        // given
        final var existingKey = Bytes.of("Key");
        final var newPassword = Bytes.of("Password");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(existingKey).fake();
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        passwordService.putPasswordEntry(existingKey, newPassword);

        // then
        then(cryptoProvider).should().encrypt(existingKey);
        then(cryptoProvider).should().encrypt(newPassword);
        then(passwordEntryRepository).should().sync();
        then(pwMan3EventRegistry).should().processEvents();
        assertThatKeyExistsWithPassword(existingKey, newPassword);
    }

    @Test
    void shouldPutPasswordEntry_RejectInvalidKey() {
        // given
        final var invalidKey = Bytes.of("1Key");

        // when
        final var actual = passwordService.putPasswordEntry(invalidKey, Bytes.of("password"));

        // then
        then(cryptoProvider).shouldHaveNoInteractions();
        then(passwordEntryRepository).shouldHaveNoInteractions();
        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isNotNull()
                .isInstanceOf(InvalidKeyException.class);
    }

    @Test
    void shouldPutPasswordEntries_InsertAndUpdate() {
        // given
        final var newKey = Bytes.of("trythis");
        final var newPassword = Bytes.of("dont use this as a password");
        final var existingKey = Bytes.of("Key");
        final var newPasswordForExistingKey = Bytes.of("Password");
        final var matchingPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(existingKey).fake();
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(matchingPasswordEntry).fake();

        // when
        passwordService.putPasswordEntries(Stream.of(
                new Tuple2<>(newKey, newPassword),
                new Tuple2<>(existingKey, newPasswordForExistingKey)
        ));

        // then
        then(cryptoProvider).should().encrypt(newKey);
        then(cryptoProvider).should().encrypt(existingKey);
        then(passwordEntryRepository).should().add(eq(PasswordEntry.create(newKey, newPassword)));
        then(passwordEntryRepository).should().sync();
        then(pwMan3EventRegistry).should().processEvents();
        assertThatKeyExistsWithPassword(existingKey, newPasswordForExistingKey);
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
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(givenPasswordEntry).fake();

        // when
        assertThat(givenPasswordEntry.viewPassword()).isEqualTo(givenPassword);
        passwordService.discardPasswordEntry(givenKey);

        // then
        then(cryptoProvider).should().encrypt(givenKey);
        then(pwMan3EventRegistry).should().processEvents();
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
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(givenPasswordEntry).fake();

        // when
        passwordService.discardPasswordEntry(otherKey);

        // then
        then(cryptoProvider).should().encrypt(otherKey);
        then(pwMan3EventRegistry).should().register(eq(new PasswordEntryNotFound(otherKey)));
        then(pwMan3EventRegistry).should().processEvents();
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
        CryptoProviderFaker.faker()
                .forInstance(cryptoProvider)
                .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
                .forInstance(passwordEntryRepository)
                .withThesePasswordEntries(passwordEntry1, passwordEntry2, passwordEntry3).fake();

        // when
        final var actual = passwordService.findAllKeys();

        // then
        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).containsExactly(key1, key3, key2);
    }

    private void assertThatKeyExistsWithPassword(final Bytes keyBytes, final Bytes passwordBytes) {
        assertThat(passwordEntryRepository.find(keyBytes))
                .isNotEmpty().get()
                .extracting(PasswordEntry::viewPassword).isNotNull()
                .isEqualTo(passwordBytes);
    }

}
