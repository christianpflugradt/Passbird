package de.pflugradts.passbird.domain.model.password;

import lombok.Value;

@Value(staticConstructor = "of")
@SuppressWarnings("checkstyle:VisibilityModifier")
public class PasswordRequirements {
    boolean includeSpecialCharacters;
    int passwordLength;
}
