package de.pflugradts.passbird.domain.service.password;

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry;
import de.pflugradts.passbird.application.security.CryptoProviderFaker;
import de.pflugradts.passbird.domain.model.password.InvalidKeyException;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.password.PasswordEntryRepositoryFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.NamespaceServiceFake;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.Tuple2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PutPasswordServiceTest {

    @Mock
    private CryptoProvider cryptoProvider;
    @Spy
    private final NamespaceServiceFake namespaceServiceFake = new NamespaceServiceFake();
    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @Mock
    private PassbirdEventRegistry passbirdEventRegistry;
    @InjectMocks
    private PutPasswordService passwordService;

    @Test
    void shouldSucceedChallengingAlphabeticAlias() {
        // given
        final var givenAlias = Bytes.bytesOf("abcDEF");

        // when
        final var actual = passwordService.challengeAlias(givenAlias);

        // then
        assertThat(actual.isSuccess()).isTrue();
    }

    @Test
    void shouldSucceedChallengingAliasWithDigitAtOtherThanFirstPosition() {
        // given
        final var givenAlias = Bytes.bytesOf("abc123");

        // when
        final var actual = passwordService.challengeAlias(givenAlias);

        // then
        assertThat(actual.isSuccess()).isTrue();
    }

    @Test
    void shouldFailChallengingAliasWithDigitAtFirstPosition() {
        // given
        final var givenAlias = Bytes.bytesOf("123abc");

        // when
        final var actual = passwordService.challengeAlias(givenAlias);

        // then
        assertThat(actual.isSuccess()).isFalse();
    }


    @Test
    void shouldFailChallengingAliasWithSpecialCharacters() {
        // given
        final var givenAlias = Bytes.bytesOf("abc!");

        // when
        final var actual = passwordService.challengeAlias(givenAlias);

        // then
        assertThat(actual.isSuccess()).isFalse();
    }

    @Test
    void shouldPutPasswordEntry_InsertNewEntry() {
        // given
        final var existingKey = Bytes.bytesOf("Key");
        final var newKey = Bytes.bytesOf("tryThis");
        final var newPassword = Bytes.bytesOf("Password");
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
        then(passwordEntryRepository).should().add(eq(PasswordEntry.create(DEFAULT, newKey, newPassword)));
        then(passbirdEventRegistry).should().processEvents();
    }

    @Test
    void shouldPutPasswordEntry_UpdateExistingEntry() {
        // given
        final var existingKey = Bytes.bytesOf("Key");
        final var newPassword = Bytes.bytesOf("Password");
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
        then(passbirdEventRegistry).should().processEvents();
        assertThatKeyExistsWithPassword(existingKey, newPassword);
    }

    @Test
    void shouldPutPasswordEntry_RejectInvalidKey() {
        // given
        final var invalidKey = Bytes.bytesOf("1Key");

        // when
        final var actual = passwordService.putPasswordEntry(invalidKey, Bytes.bytesOf("password"));

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
        final var newKey = Bytes.bytesOf("trythis");
        final var newPassword = Bytes.bytesOf("dont use this as a password");
        final var existingKey = Bytes.bytesOf("Key");
        final var newPasswordForExistingKey = Bytes.bytesOf("Password");
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
        then(passwordEntryRepository).should().add(eq(PasswordEntry.create(DEFAULT, newKey, newPassword)));
        then(passwordEntryRepository).should().sync();
        then(passbirdEventRegistry).should().processEvents();
        assertThatKeyExistsWithPassword(existingKey, newPasswordForExistingKey);
    }

    private void assertThatKeyExistsWithPassword(final Bytes keyBytes, final Bytes passwordBytes) {
        assertThat(passwordEntryRepository.find(keyBytes))
            .isNotEmpty().get()
            .extracting(PasswordEntry::viewPassword).isNotNull()
            .isEqualTo(passwordBytes);
    }

}
