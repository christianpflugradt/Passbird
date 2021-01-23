package de.pflugradts.pwman3.domain.model.transfer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
public class CharValue {

    public static final int MIN_ASCII_VALUE = 32;
    public static final int MAX_ASCII_VALUE = 126;
    public static final int FIRST_DIGIT_INDEX = 48;
    public static final int LAST_DIGIT_INDEX = 57;
    public static final int FIRST_UPPERCASE_INDEX = 65;
    public static final int LAST_UPPERCASE_INDEX = 90;
    public static final int FIRST_LOWERCASE_INDEX = 97;
    public static final int LAST_LOWERCASE_INDEX = 122;

    private final char value;

    public static CharValue of(final byte b) {
        return of((char) b);
    }

    public static CharValue of(final int i) {
        return of((char) i);
    }

    public boolean isDigit() {
        return value >= FIRST_DIGIT_INDEX && value <= LAST_DIGIT_INDEX;
    }

    public boolean isUppercaseCharacter() {
        return value >= FIRST_UPPERCASE_INDEX && value <= LAST_UPPERCASE_INDEX;
    }

    public boolean isLowercaseCharacter() {
        return value >= FIRST_LOWERCASE_INDEX && value <= LAST_LOWERCASE_INDEX;
    }

    public boolean isAlphabeticCharacter() {
        return isUppercaseCharacter() || isLowercaseCharacter();
    }

    public boolean isSymbol() {
        return !(isDigit() || isUppercaseCharacter() || isLowercaseCharacter());
    }

}
