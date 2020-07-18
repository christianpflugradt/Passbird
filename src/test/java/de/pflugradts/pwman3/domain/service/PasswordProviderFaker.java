package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.password.PasswordRequirements;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.provider.PasswordProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordProviderFaker {

    private PasswordProvider passwordProvider;

    public static PasswordProviderFaker faker() {
        return new PasswordProviderFaker();
    }

    public PasswordProviderFaker forInstance(final PasswordProvider passwordProvider) {
        this.passwordProvider = passwordProvider;
        return this;
    }

    public PasswordProviderFaker withCreatingThisPassword(final Bytes password) {
        given(passwordProvider.createNewPassword(any(PasswordRequirements.class))).willReturn(password);
        return this;
    }

    public PasswordProvider fake() {
        return passwordProvider;
    }

}
