package de.pflugradts.passbird.application.security;

import de.pflugradts.passbird.application.util.ByteArrayUtils;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import io.vavr.control.Try;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import static de.pflugradts.passbird.application.util.CryptoUtils.AES_ENCRYPTION;
import static de.pflugradts.passbird.application.util.CryptoUtils.AES_CBC_PKCS5PADDING_CIPHER;
import static de.pflugradts.passbird.application.util.CryptoUtils.BLOCK_SIZE;
import static de.pflugradts.passbird.application.util.CryptoUtils.SHA512_HASH;

public class Cipherizer implements CryptoProvider {

    private final Bytes keyBytes;
    private final Bytes ivBytes;

    Cipherizer(final Bytes keyBytes, final Bytes ivBytes) {
        this.keyBytes = keyBytes;
        this.ivBytes = ivBytes;
    }

    @Override
    public Try<Bytes> encrypt(final Bytes bytes) {
        return Try.of(() -> this.cipherize(Cipher.ENCRYPT_MODE, pack(bytes)));
    }

    @Override
    public Try<Bytes> decrypt(final Bytes bytes) {
        return Try.of(() -> unpack(this.cipherize(Cipher.DECRYPT_MODE, bytes)));
    }

    private Bytes cipherize(final int mode, final Bytes bytes)
            throws BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        final var cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING_CIPHER);
        cipher.init(mode,
                new SecretKeySpec(Arrays.copyOf(MessageDigest.getInstance(SHA512_HASH)
                        .digest(keyBytes.toByteArray()), BLOCK_SIZE), AES_ENCRYPTION),
                new IvParameterSpec(ivBytes.toByteArray()));
        return Bytes.bytesOf(cipher.doFinal(bytes.toByteArray()));
    }

    private int calcPadding(final Bytes bytes) {
        return BLOCK_SIZE - bytes.getSize() % BLOCK_SIZE;
    }

    private Bytes pack(final Bytes bytes) {
        final int padding = calcPadding(bytes);
        final byte[] target = new byte[bytes.getSize() + padding];
        ByteArrayUtils.copyBytes(bytes, target, 0);
        target[target.length - 1] = (byte) padding;
        return Bytes.bytesOf(target);
    }

    private Bytes unpack(final Bytes bytes) {
        final int padding = bytes.getByte(bytes.getSize() - 1);
        final byte[] target = new byte[bytes.getSize() - padding];
        ByteArrayUtils.copyBytes(bytes, target, 0, target.length);
        return Bytes.bytesOf(target);
    }

}
