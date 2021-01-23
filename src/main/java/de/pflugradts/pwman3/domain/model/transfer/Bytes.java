package de.pflugradts.pwman3.domain.model.transfer;

import de.pflugradts.pwman3.domain.model.ddd.ValueObject;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.pflugradts.pwman3.domain.model.transfer.CharValue.MAX_ASCII_VALUE;
import static de.pflugradts.pwman3.domain.model.transfer.CharValue.MIN_ASCII_VALUE;

/**
 * <p>A Bytes is the basic structure to represent information.</p>
 * <p>Any content, be it public or sensitive data, should always be represented as Bytes.
 * A Bytes can be constructed from and converted to other data structures such as String, byte[] or
 * {@link Chars}. Sensitive data should never be converted to String unless it's inevitable.</p>
 * <p>A {@link CryptoProvider CryptoProvider} can encrypt/decrypt a Bytes.
 * On an unencrypted Bytes {@link #scramble()} should always be called after using it.</p>
 */
@EqualsAndHashCode(of = "byteArray")
@ToString(of = "byteArray")
@SuppressWarnings("PMD.TooManyMethods")
public class Bytes implements ValueObject, Iterable<Byte> {

    private final byte[] byteArray;
    private final Iterator<Byte> byteIterator;

    private Bytes(final byte... bytes) {
        final var clonedBytes = bytes.clone();
        this.byteArray = clonedBytes;
        this.byteIterator = new BytesIterator(clonedBytes);
    }

    public static Bytes of(final byte... bytes) {
        return new Bytes(bytes);
    }

    public static Bytes of(final List<Byte> b) {
        final var bytes = new byte[b.size()];
        for (int i = 0; i < b.size(); i++) {
            bytes[i] = b.get(i);
        }
        return Bytes.of(bytes);
    }

    public static Bytes of(final String s) {
        return Chars.of(s.toCharArray()).toBytes();
    }

    public static Bytes empty() {
        return Bytes.of();
    }

    public int size() {
        return byteArray.length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public byte getByte(final int index) {
        return byteArray[index];
    }

    public Bytes slice(final int fromInclusive, final int toExclusive) {
        if (toExclusive - fromInclusive > 0) {
            final var sub = new byte[toExclusive - fromInclusive];
            System.arraycopy(byteArray, fromInclusive, sub, 0, sub.length);
            return Bytes.of(sub);
        } else {
            return Bytes.empty();
        }
    }

    public byte getFirstByte() {
        return byteArray[0];
    }

    public void scramble() {
        final var random = new SecureRandom();
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[i] = (byte) (random.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE);
        }
    }

    public byte[] toByteArray() {
        return byteArray.clone();
    }

    public Chars toChars() {
        final var c = new char[size()];
        for (int i = 0; i < size(); i++) {
            c[i] = (char) byteArray[i];
        }
        return Chars.of(c);
    }

    public String asString() {
        final var builder = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            builder.append((char) byteArray[i]);
        }
        return builder.toString();
    }

    public Stream<Byte> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Bytes copy() {
        return Bytes.of(byteArray.clone());
    }

    @Override
    public Iterator<Byte> iterator() {
        return byteIterator;
    }

    public static final class BytesIterator implements Iterator<Byte> {

        private int index;
        private final byte[] bytes;

        private BytesIterator(final byte... bytes) {
            this.bytes = bytes.clone();
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < bytes.length;
        }

        @Override
        public Byte next() {
            if (hasNext()) {
                return bytes[index++];
            } else {
                throw new NoSuchElementException();
            }
        }

    }

}
