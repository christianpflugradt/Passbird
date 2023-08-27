package de.pflugradts.passbird.domain.model.transfer;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CharValueTest {

    private static final String DIGITS = "0123456789";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String SYMBOLS = "$&[{}(=*)+]!#/@\\';,.-~%`?^|\":<>_ ";

    @Test
    void shouldDetectDigits() {
        // given / when / then
        stream(DIGITS).forEach(c ->
            assertThat(CharValue.of(c).isDigit()).isTrue());

        stream(UPPER).forEach(c ->
            assertThat(CharValue.of(c).isDigit()).isFalse());
        stream(LOWER).forEach(c ->
            assertThat(CharValue.of(c).isDigit()).isFalse());
        stream(SYMBOLS).forEach(c ->
            assertThat(CharValue.of(c).isDigit()).isFalse());
    }

    @Test
    void shouldDetectUppercase() {
        // given / when / then
        stream(UPPER).forEach(c ->
            assertThat(CharValue.of(c).isUppercaseCharacter()).isTrue());

        stream(DIGITS).forEach(c ->
            assertThat(CharValue.of(c).isUppercaseCharacter()).isFalse());
        stream(LOWER).forEach(c ->
            assertThat(CharValue.of(c).isUppercaseCharacter()).isFalse());
        stream(SYMBOLS).forEach(c ->
            assertThat(CharValue.of(c).isUppercaseCharacter()).isFalse());
    }

    @Test
    void shouldDetectLowercase() {
        // given / when / then
        stream(LOWER).forEach(c ->
            assertThat(CharValue.of(c).isLowercaseCharacter()).isTrue());

        stream(DIGITS).forEach(c ->
            assertThat(CharValue.of(c).isLowercaseCharacter()).isFalse());
        stream(UPPER).forEach(c ->
            assertThat(CharValue.of(c).isLowercaseCharacter()).isFalse());
        stream(SYMBOLS).forEach(c ->
            assertThat(CharValue.of(c).isLowercaseCharacter()).isFalse());
    }

    @Test
    void shouldDetectSymbols() {
        // given / when / then
        stream(SYMBOLS).forEach(c ->
            assertThat(CharValue.of(c).isSymbol()).isTrue());

        stream(DIGITS).forEach(c ->
            assertThat(CharValue.of(c).isSymbol()).isFalse());
        stream(UPPER).forEach(c ->
            assertThat(CharValue.of(c).isSymbol()).isFalse());
        stream(LOWER).forEach(c ->
            assertThat(CharValue.of(c).isSymbol()).isFalse());
    }

    private Stream<Character> stream(String string) {
        return string.chars().mapToObj(i -> (char) i);
    }

}
