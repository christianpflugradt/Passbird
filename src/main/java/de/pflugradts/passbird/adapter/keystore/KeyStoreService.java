package de.pflugradts.passbird.adapter.keystore;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.security.Key;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Chars;
import io.vavr.control.Try;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import static de.pflugradts.passbird.application.util.CryptoUtils.AES_ENCRYPTION;
import static de.pflugradts.passbird.application.util.CryptoUtils.KEYSTORE_KEY_BITS;

@NoArgsConstructor
@AllArgsConstructor
public class KeyStoreService implements KeyStoreAdapterPort {

    private static final String SECRET_ALIAS = "PwMan3Secret";
    private static final String IV_ALIAS = "PwMan3IV";

    @Inject
    private SystemOperation systemOperation;

    @Override
    public Try<Key> loadKey(final Chars password, final Path path) {
        return Try.of(() -> load(password, systemOperation.newInputStream(path)));
    }

    @SuppressWarnings("checkstyle:IllegalThrows")
    private Key load(final Chars password, final Try<InputStream> tryInputStream)
            throws Throwable {
        final var tryKeyStore = systemOperation.getJceksInstance();
        if (tryInputStream.isSuccess() && tryKeyStore.isSuccess()) {
            try (var inputStream = tryInputStream.get()) {
                final var keyStore = tryKeyStore.get();
                final var passwordChars = password.toCharArray();
                keyStore.load(inputStream, passwordChars);
                final var secret = keyStore.getKey(SECRET_ALIAS, passwordChars);
                final var iv = keyStore.getKey(IV_ALIAS, passwordChars);
                Chars.scramble(passwordChars);
                password.scrambleSelf();
                return new Key(Bytes.of(secret.getEncoded()), Bytes.of(iv.getEncoded()));
            }
        } else if (tryKeyStore.isFailure()) {
            throw tryKeyStore.getCause();
        } else {
            throw tryInputStream.getCause();
        }
    }

    @Override
    public Try<Void> storeKey(final Chars password, final Path path) {
        return Try.run(() -> store(password, systemOperation.newOutputStream(path)));
    }

    @SuppressWarnings("checkstyle:IllegalThrows")
    private void store(final Chars password, final Try<OutputStream> tryOutputStream)
            throws Throwable {
        final var tryKeyStore = systemOperation.getJceksInstance();
        if (tryOutputStream.isSuccess() && tryKeyStore.isSuccess()) {
            try (var outputStream = tryOutputStream.get()) {
                final var keyStore = tryKeyStore.get();
                final var passwordChars = password.toCharArray();
                keyStore.load(null, null);
                final var keyGenerator = KeyGenerator.getInstance(AES_ENCRYPTION);
                keyGenerator.init(KEYSTORE_KEY_BITS);
                keyStore.setEntry(
                        SECRET_ALIAS,
                        new KeyStore.SecretKeyEntry(keyGenerator.generateKey()),
                        new KeyStore.PasswordProtection(passwordChars));
                keyStore.setEntry(
                        IV_ALIAS,
                        new KeyStore.SecretKeyEntry(keyGenerator.generateKey()),
                        new KeyStore.PasswordProtection(passwordChars));
                keyStore.store(outputStream, passwordChars);
                Chars.scramble(passwordChars);
                password.scrambleSelf();
            }
        } else if (tryKeyStore.isFailure()) {
            throw tryKeyStore.getCause();
        } else {
            throw tryOutputStream.getCause();
        }
    }

}
