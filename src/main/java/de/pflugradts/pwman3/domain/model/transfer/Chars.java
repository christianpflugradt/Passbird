package de.pflugradts.pwman3.domain.model.transfer;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.security.SecureRandom;

import static de.pflugradts.pwman3.domain.model.transfer.CharValue.MAX_ASCII_VALUE;
import static de.pflugradts.pwman3.domain.model.transfer.CharValue.MIN_ASCII_VALUE;

/**
 * <p>A Chars is used to represent data send to or received from external interfaces that uses char[].</p>
 * <p>A Chars can be constructed from and converted to a {@link Bytes}
 * which is basic structure used to represent data.</p>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class Chars {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final char[] charArray;

    public static Chars of(final char... chars) {
        return new Chars(chars);
    }

    public static void scramble(char... chars) {
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (SECURE_RANDOM.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE);
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
