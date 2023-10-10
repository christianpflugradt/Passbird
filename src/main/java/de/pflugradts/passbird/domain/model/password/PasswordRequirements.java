package de.pflugradts.passbird.domain.model.password;

public class PasswordRequirements {
    boolean includeSpecialCharacters;
    int passwordLength;
    private PasswordRequirements(boolean includeSpecialCharacters, int passwordLength) {
        this.includeSpecialCharacters = includeSpecialCharacters;
        this.passwordLength = passwordLength;
    }
    public static PasswordRequirements of(boolean includeSpecialCharacters, int passwordLength) {
        return new PasswordRequirements(includeSpecialCharacters, passwordLength);
    }

    public boolean isIncludeSpecialCharacters() {
        return includeSpecialCharacters;
    }

    public int getPasswordLength() {
        return passwordLength;
    }
}
