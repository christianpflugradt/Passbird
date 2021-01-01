package de.pflugradts.pwman3.domain.service.password;

import de.pflugradts.pwman3.application.eventhandling.PwMan3EventRegistry;
import de.pflugradts.pwman3.application.security.CryptoProviderFaker;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryRepositoryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
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
    private PwMan3EventRegistry pwMan3EventRegistry;
    @InjectMocks
    private DiscardPasswordService passwordService;

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

}
