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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DiscardPasswordServiceTest {

    @Mock
    private CryptoProvider cryptoProvider;
    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @Mock
    private PassbirdEventRegistry passbirdEventRegistry;
    @InjectMocks
    private DiscardPasswordService passwordService;

    @Test
    void shouldDiscardPasswordEntry() {
        // given
        final var givenKey = Bytes.bytesOf("Key");
        final var givenPassword = Bytes.bytesOf("Password");
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
        then(passbirdEventRegistry).should().processEvents();
        assertThat(givenPasswordEntry.viewPassword()).isNotNull().isNotEqualTo(givenPassword);
    }

    @Test
    void shouldDiscardPasswordEntry_NoMatch() {
        // given
        final var givenKey = Bytes.bytesOf("Key");
        final var otherKey = Bytes.bytesOf("try this");
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
        then(passbirdEventRegistry).should().register(eq(new PasswordEntryNotFound(otherKey)));
        then(passbirdEventRegistry).should().processEvents();
    }

}
