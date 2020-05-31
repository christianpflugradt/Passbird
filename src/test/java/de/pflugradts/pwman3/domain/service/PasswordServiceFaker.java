package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordServiceFaker {

    private PasswordService passwordService = mock(PasswordService.class);
    private List<PasswordEntry> passwordEntries = new ArrayList<>();

    public static PasswordServiceFaker faker() {
        return new PasswordServiceFaker();
    }

    public PasswordServiceFaker forInstance(final PasswordService passwordService) {
        this.passwordService = passwordService;
        return this;
    }

    public PasswordServiceFaker withPasswordEntries(final PasswordEntry... passwordEntries) {
        this.passwordEntries.clear();
        this.passwordEntries.addAll(Arrays.asList(passwordEntries));
        return this;
    }

    public PasswordService fake() {
        lenient().when(passwordService.findAllKeys())
                .thenReturn(passwordEntries.stream().map(PasswordEntry::viewKey));
        passwordEntries.forEach(passwordEntry -> {
                lenient().when(passwordService.viewPassword(passwordEntry.viewKey()))
                        .thenReturn(Optional.of(passwordEntry.viewPassword()));
                lenient().when(passwordService.entryExists(passwordEntry.viewKey()))
                        .thenReturn(true);
        });
        return passwordService;
    }

}
