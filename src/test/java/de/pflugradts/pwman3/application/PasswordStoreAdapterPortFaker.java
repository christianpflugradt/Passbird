package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordStoreAdapterPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordStoreAdapterPortFaker {

    private PasswordStoreAdapterPort passwordStoreAdapterPort = mock(PasswordStoreAdapterPort.class);
    private List<PasswordEntry> passwordEntries = new ArrayList<>();

    public static PasswordStoreAdapterPortFaker faker() {
        return new PasswordStoreAdapterPortFaker();
    }

    public PasswordStoreAdapterPortFaker forInstance(final PasswordStoreAdapterPort passwordStoreAdapterPort) {
        this.passwordStoreAdapterPort = passwordStoreAdapterPort;
        return this;
    }

    public PasswordStoreAdapterPortFaker withEmptyPasswordEntriesList() {
        passwordEntries.clear();
        return this;
    }

    public PasswordStoreAdapterPortFaker withThesePasswordEntries(final PasswordEntry... passwordEntries) {
        this.passwordEntries.clear();
        Collections.addAll(this.passwordEntries, passwordEntries);
        return this;
    }

    public PasswordStoreAdapterPort fake() {
        given(passwordStoreAdapterPort.restore()).willReturn(() -> passwordEntries.stream());
        return passwordStoreAdapterPort;
    }

}
