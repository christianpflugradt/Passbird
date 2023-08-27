package de.pflugradts.passbird.domain.model.password;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordRequirementsFaker {

    private int passwordLength;
    private boolean includeSpecialCharacters;

    public static PasswordRequirementsFaker faker() {
        return new PasswordRequirementsFaker();
    }

    public PasswordRequirementsFaker fakePasswordRequirements() {
        passwordLength = 20;
        includeSpecialCharacters = true;
        return this;
    }

    public PasswordRequirementsFaker withPasswordLength(final int passwordLength) {
        this.passwordLength = passwordLength;
        return this;
    }

    public PasswordRequirementsFaker withUseSpecialCharactersEnabled() {
        this.includeSpecialCharacters = true;
        return this;
    }

    public PasswordRequirementsFaker withUseSpecialCharactersDisabled() {
        this.includeSpecialCharacters = false;
        return this;
    }

    public PasswordRequirements fake() {
        return PasswordRequirements.of(includeSpecialCharacters, passwordLength);
    }

}
