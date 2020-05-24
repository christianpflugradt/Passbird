package de.pflugradts.pwman3.application.util;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AsciiUtilsTest {

    private static final String DIGITS = "0123456789";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String SYMBOLS = "$&[{}(=*)+]!#/@\\';,.-~%`?^|\":<>_ ";

    @Test
    void shouldDetectDigits() {
        // given / when / then
        stream(DIGITS).forEach(c ->
                assertThat(AsciiUtils.isDigit(c)).isTrue());

        stream(UPPER).forEach(c ->
                assertThat(AsciiUtils.isDigit(c)).isFalse());
        stream(LOWER).forEach(c ->
                assertThat(AsciiUtils.isDigit(c)).isFalse());
        stream(SYMBOLS).forEach(c ->
                assertThat(AsciiUtils.isDigit(c)).isFalse());
    }

    @Test
    void shouldDetectUppercase() {
        // given / when / then
        stream(UPPER).forEach(c ->
                assertThat(AsciiUtils.isUppercaseCharacter(c)).isTrue());

        stream(DIGITS).forEach(c ->
                assertThat(AsciiUtils.isUppercaseCharacter(c)).isFalse());
        stream(LOWER).forEach(c ->
                assertThat(AsciiUtils.isUppercaseCharacter(c)).isFalse());
        stream(SYMBOLS).forEach(c ->
                assertThat(AsciiUtils.isUppercaseCharacter(c)).isFalse());
    }

    @Test
    void shouldDetectLowercase() {
        // given / when / then
        stream(LOWER).forEach(c ->
                assertThat(AsciiUtils.isLowercaseCharacter(c)).isTrue());

        stream(DIGITS).forEach(c ->
                assertThat(AsciiUtils.isLowercaseCharacter(c)).isFalse());
        stream(UPPER).forEach(c ->
                assertThat(AsciiUtils.isLowercaseCharacter(c)).isFalse());
        stream(SYMBOLS).forEach(c ->
                assertThat(AsciiUtils.isLowercaseCharacter(c)).isFalse());
    }

    @Test
    void shouldDetectSymbols() {
        // given / when / then
        stream(SYMBOLS).forEach(c ->
                assertThat(AsciiUtils.isSymbol(c)).isTrue());

        stream(DIGITS).forEach(c ->
                assertThat(AsciiUtils.isSymbol(c)).isFalse());
        stream(UPPER).forEach(c ->
                assertThat(AsciiUtils.isSymbol(c)).isFalse());
        stream(LOWER).forEach(c ->
                assertThat(AsciiUtils.isSymbol(c)).isFalse());
    }

    private Stream<Character> stream(String string) {
        return string.chars().mapToObj(i -> (char) i);
    }

}
