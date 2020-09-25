package de.pflugradts.pwman3.domain.service.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class AsciiUtils {
    public static final int MIN_ASCII_VALUE = 32;
    public static final int MAX_ASCII_VALUE = 126;
    public static final int FIRST_DIGIT_INDEX = 48;
    public static final int LAST_DIGIT_INDEX = 57;
    public static final int FIRST_UPPERCASE_INDEX = 65;
    public static final int LAST_UPPERCASE_INDEX = 90;
    public static final int FIRST_LOWERCASE_INDEX = 97;
    public static final int LAST_LOWERCASE_INDEX = 122;

    public static boolean isDigit(final int i) {
        return i >= FIRST_DIGIT_INDEX && i <= LAST_DIGIT_INDEX;
    }

    public static boolean isUppercaseCharacter(final int i) {
        return i >= FIRST_UPPERCASE_INDEX && i <= LAST_UPPERCASE_INDEX;
    }

    public static boolean isLowercaseCharacter(final int i) {
        return i >= FIRST_LOWERCASE_INDEX && i <= LAST_LOWERCASE_INDEX;
    }

    public static boolean isSymbol(final int i) {
        return !(isDigit(i) || isUppercaseCharacter(i) || isLowercaseCharacter(i));
    }

}
