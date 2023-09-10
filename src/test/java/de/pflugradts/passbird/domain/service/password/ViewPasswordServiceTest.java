package de.pflugradts.passbird.domain.service.password;

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry;
import de.pflugradts.passbird.application.security.CryptoProviderFaker;
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.password.PasswordEntryRepositoryFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.DO_NOTHING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ViewPasswordServiceTest {

    @Mock
    private CryptoProvider cryptoProvider;
    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @Mock
    private PassbirdEventRegistry passbirdEventRegistry;
    @InjectMocks
    private ViewPasswordService passwordService;

    @Test
    void shouldReturnTrue_IfEntryExists() {
        // given
        final var givenKey = Bytes.bytesOf("Key");
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
        final var actual = passwordService.entryExists(givenKey, DO_NOTHING);

        // then
        then(passbirdEventRegistry).shouldHaveNoInteractions();
        assertThat(actual).isTrue();
    }

    @Test
    void shouldReturnFalse_IfEntryDoesNotExist() {
        // given
        final var givenKey = Bytes.bytesOf("Key");
        final var otherKey = Bytes.bytesOf("try this");
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
        final var actual = passwordService.entryExists(otherKey, DO_NOTHING);

        // then
        then(passbirdEventRegistry).shouldHaveNoInteractions();
        assertThat(actual).isFalse();
    }

    @Test
    void shouldFindExistingPassword() {
        // given
        final var givenKey = Bytes.bytesOf("Key");
        final var expectedPassword = Bytes.bytesOf("Password");
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
        then(passbirdEventRegistry).shouldHaveNoInteractions();
        assertThat(actual).isNotEmpty();
        assertThat(actual).contains(expectedPassword);
    }

    @Test
    void shouldFindExistingPassword_ReturnEmptyOptionalOnNoMatch() {
        // given
        final var givenKey = Bytes.bytesOf("Key");
        final var otherKey = Bytes.bytesOf("tryThis");
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
        then(passbirdEventRegistry).should().register(eq(new PasswordEntryNotFound(otherKey)));
        then(passbirdEventRegistry).should().processEvents();
        assertThat(actual).isEmpty();
    }

    @Test
    void shouldFindAllKeysAlphabetically() {
        // given
        final var key1 = Bytes.bytesOf("abc");
        final var passwordEntry1 = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(key1).fake();
        final var key2 = Bytes.bytesOf("xyz");
        final var passwordEntry2 = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(key2).fake();
        final var key3 = Bytes.bytesOf("hij");
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
        assertThat(actual).containsExactly(key1, key3, key2);
    }

}
