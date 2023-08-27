package de.pflugradts.passbird.adapter.passwordstore;

import de.pflugradts.passbird.domain.model.transfer.Bytes;

import java.util.stream.StreamSupport;

import static java.lang.Integer.BYTES;

class PasswordStoreCommons {

    static final Integer EOF = 0;
    static final Integer SECTOR = -1;
    static final Integer EMPTY_NAMESPACE = -2;

    byte checksum(final byte[] bytes) {
        return (byte) StreamSupport.stream(Bytes.of(bytes).spliterator(), false)
            .mapToInt(b -> (int) (byte) b)
            .reduce(0, Integer::sum);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    byte[] signature() {
        return new byte[]{0x0, 0x50, 0x77, 0x4D, 0x61, 0x6E, 0x34, 0x0};
    }

    int signatureSize() {
        return signature().length;
    }

    int intBytes() {
        return BYTES;
    }

    int eofBytes() {
        return intBytes();
    }

    int checksumBytes() {
        return 1;
    }

}
