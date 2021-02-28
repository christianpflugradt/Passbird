package de.pflugradts.pwman3.domain.service.password;

import de.pflugradts.pwman3.application.eventhandling.PwMan3EventRegistry;
import de.pflugradts.pwman3.application.security.CryptoProviderFaker;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.password.KeyAlreadyExistsException;
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

@ExtendWith(MockitoExtension.class)
class MovePasswordServiceTest {

    @Mock
    private CryptoProvider cryptoProvider;
    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @Mock
    private PwMan3EventRegistry pwMan3EventRegistry;
    @InjectMocks
    private MovePasswordService passwordService;

    @Test
    void shouldMovePasswordEntry() {
        // given
        final var givenKey = Bytes.of("key123");
        final var givenNamespace = NamespaceSlot._1;
        final var newNamespace = NamespaceSlot._2;
        final var givenPasswordEntry = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(givenKey)
            .withNamespace(givenNamespace).fake();
        CryptoProviderFaker.faker()
            .forInstance(cryptoProvider)
            .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
            .forInstance(passwordEntryRepository)
            .withThesePasswordEntries(givenPasswordEntry).fake();

        // when
        final var actual = passwordService.movePassword(givenKey, newNamespace);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.isSuccess()).isTrue();
        assertThat(givenPasswordEntry.associatedNamespace())
            .isNotNull().isEqualTo(newNamespace).isNotEqualTo(givenNamespace);
    }

    @Test
    void shouldNotMovePasswordEntryIfAlreadyExistsInTargetNamespace() {
        // given
        final var givenKey = Bytes.of("key123");
        final var givenNamespace = NamespaceSlot._1;
        final var newNamespace = NamespaceSlot._2;
        final var givenPasswordEntry = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(givenKey)
            .withNamespace(givenNamespace).fake();
        final var conflictingPasswordEntry = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(givenKey)
            .withNamespace(newNamespace).fake();
        CryptoProviderFaker.faker()
            .forInstance(cryptoProvider)
            .withMockedEncryption().fake();
        PasswordEntryRepositoryFaker.faker()
            .forInstance(passwordEntryRepository)
            .withThesePasswordEntries(givenPasswordEntry, conflictingPasswordEntry).fake();

        // when
        final var actual = passwordService.movePassword(givenKey, newNamespace);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.isSuccess()).isFalse();
        assertThat(actual.getCause()).isNotNull().isInstanceOf(KeyAlreadyExistsException.class);
        assertThat(givenPasswordEntry.associatedNamespace())
            .isNotNull().isNotEqualTo(newNamespace).isEqualTo(givenNamespace);
    }

}
