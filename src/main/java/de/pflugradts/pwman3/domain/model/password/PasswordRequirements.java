package de.pflugradts.pwman3.domain.model.password;

import lombok.Value;

@Value(staticConstructor = "of")
@SuppressWarnings("checkstyle:VisibilityModifier")
public class PasswordRequirements {
    boolean includeSpecialCharacters;
    int passwordLength;
}
