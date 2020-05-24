package de.pflugradts.pwman3.application.util;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public final class ByteArrayUtils {

    private static byte[] asByteArray(final int i) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(i).array();
    }

    private static int byteArrayAsInt(final byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }

    public static Integer readInt(final byte[] array, final int offset) {
        return byteArrayAsInt(readBytes(array, offset, Integer.BYTES));
    }

    public static byte[] readBytes(final byte[] array, final int offset, final int size) {
        return Arrays.copyOfRange(array, offset, size + offset);
    }

    public static int copyBytes(final int i, final byte[] target, final int offset) {
        return copyBytes(asByteArray(i), target, offset, Integer.BYTES);
    }

    public static int copyBytes(final Bytes source, final byte[] target, final int offset) {
        return copyBytes(source, target, offset, Math.min(source.size(), target.length));
    }

    public static int copyBytes(final Bytes source, final byte[] target, final int offset, final int size) {
        return copyBytes(source.toByteArray(), target, offset, size);
    }

    public static int copyBytes(final byte[] source, final byte[] target, final int offset, final int size) {
        System.arraycopy(source, 0, target, offset, size);
        return size;
    }

}
