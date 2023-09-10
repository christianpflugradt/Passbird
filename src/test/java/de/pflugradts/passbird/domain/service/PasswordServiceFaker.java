package de.pflugradts.passbird.domain.service;

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.InvalidKeyException;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordServiceFaker {

    private PasswordService passwordService = mock(PasswordService.class);
    private Bytes invalidAlias = null;
    private final List<PasswordEntry> passwordEntries = new ArrayList<>();

    public static PasswordServiceFaker faker() {
        return new PasswordServiceFaker();
    }

    public PasswordServiceFaker forInstance(final PasswordService passwordService) {
        this.passwordService = passwordService;
        return this;
    }

    public PasswordServiceFaker withInvalidAlias(final Bytes invalidAlias) {
        this.invalidAlias = invalidAlias;
        return this;
    }

    public PasswordServiceFaker withPasswordEntries(final PasswordEntry... passwordEntries) {
        this.passwordEntries.clear();
        this.passwordEntries.addAll(Arrays.asList(passwordEntries));
        return this;
    }

    public PasswordService fake() {
        if (invalidAlias != null) {
            lenient().doThrow(new InvalidKeyException(invalidAlias)).when(passwordService).challengeAlias(invalidAlias);
        }
        lenient().when(passwordService.findAllKeys())
                .thenReturn(passwordEntries.stream().map(PasswordEntry::viewKey));
        lenient().when(passwordService.entryExists(any(Bytes.class), any(PasswordService.EntryNotExistsAction.class)))
                .thenReturn(false);
        lenient().when(passwordService.entryExists(any(Bytes.class), any(NamespaceSlot.class)))
                .thenReturn(false);
        passwordEntries.forEach(passwordEntry -> {
                lenient().when(passwordService.viewPassword(passwordEntry.viewKey()))
                        .thenReturn(Optional.of(passwordEntry.viewPassword()));
                lenient().when(passwordService.entryExists(
                        eq(passwordEntry.viewKey()),
                        any(PasswordService.EntryNotExistsAction.class))
                ).thenReturn(true);
                lenient().when(passwordService.entryExists(
                    eq(passwordEntry.viewKey()),
                    eq(passwordEntry.associatedNamespace()))
                ).thenReturn(true);
        });
        return passwordService;
    }

}
