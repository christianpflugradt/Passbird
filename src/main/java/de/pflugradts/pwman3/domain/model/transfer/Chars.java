package de.pflugradts.pwman3.domain.model.transfer;

import java.security.SecureRandom;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import static de.pflugradts.pwman3.application.util.AsciiUtils.MAX_ASCII_VALUE;
import static de.pflugradts.pwman3.application.util.AsciiUtils.MIN_ASCII_VALUE;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class Chars {

    private final char[] charArray;

    public static Chars of(final char... chars) {
        return new Chars(chars);
    }

    public static void scramble(char... chars) {
        final var random = new SecureRandom();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (random.nextInt(MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE);
        }
    }

    public Bytes toBytes() {
        final var byteArray = new byte[charArray.length];
        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }
        scrambleSelf();
        return Bytes.of(byteArray);
    }

    public void scrambleSelf() {
        scramble(charArray);
    }

    public char[] toCharArray() {
        return charArray.clone();
    }

}
