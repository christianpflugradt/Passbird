package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordEntryRepositoryFaker {

    private PasswordEntryRepository passwordEntryRepository;
    private List<PasswordEntry> passwordEntries = new ArrayList<>();

    public static PasswordEntryRepositoryFaker faker() {
        return new PasswordEntryRepositoryFaker();
    }

    public PasswordEntryRepositoryFaker forInstance(final PasswordEntryRepository passwordEntryRepository) {
        this.passwordEntryRepository = passwordEntryRepository;
        return this;
    }

    public PasswordEntryRepositoryFaker withThesePasswordEntries(final PasswordEntry... passwordEntries) {
        this.passwordEntries.clear();
        Collections.addAll(this.passwordEntries, passwordEntries);
        return this;
    }

    public PasswordEntryRepository fake() {
        lenient().when(passwordEntryRepository.find(any(Bytes.class))).thenAnswer(
                invocation -> passwordEntries.stream().filter(
                        passwordEntry -> passwordEntry.viewKey().equals(invocation.getArgument(0))).findAny());
        lenient().when(passwordEntryRepository.find(any(Bytes.class), any(NamespaceSlot.class))).thenAnswer(
            invocation -> passwordEntries.stream().filter(
                passwordEntry -> passwordEntry.viewKey().equals(invocation.getArgument(0))
                    && passwordEntry.associatedNamespace().equals(invocation.getArgument(1))).findAny());
        lenient().when(passwordEntryRepository.findAll()).thenReturn(passwordEntries.stream());
        return passwordEntryRepository;
    }

}
