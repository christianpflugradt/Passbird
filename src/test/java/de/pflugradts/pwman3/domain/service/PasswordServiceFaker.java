package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.PasswordService;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import static org.mockito.ArgumentMatchers.any;
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
                .thenReturn(Try.of(() -> passwordEntries.stream().map(PasswordEntry::viewKey)));
        lenient().when(passwordService.entryExists(any(Bytes.class))).thenReturn(Try.of(() -> false));
        passwordEntries.forEach(passwordEntry -> {
                lenient().when(passwordService.viewPassword(passwordEntry.viewKey()))
                        .thenReturn(Optional.of(Try.of(passwordEntry::viewPassword)));
                lenient().when(passwordService.entryExists(passwordEntry.viewKey()))
                        .thenReturn(Try.of(() -> true));
                lenient().when(passwordService.discardPasswordEntry(passwordEntry.viewKey()))
                        .thenReturn(Try.success(null));
        });
        return passwordService;
    }

}
