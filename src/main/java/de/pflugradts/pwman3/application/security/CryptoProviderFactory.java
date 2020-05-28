package de.pflugradts.pwman3.application.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.adapter.keystore.Key;
import de.pflugradts.pwman3.application.KeyStoreAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Chars;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import io.vavr.control.Try;
import javax.security.auth.login.LoginException;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.KEYSTORE_FILENAME;

@Singleton
public class CryptoProviderFactory {

    @Inject
    private Bootable application;
    @Inject
    private FailureCollector failureCollector;
    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private KeyStoreAdapterPort keyStoreAdapterPort;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private SystemOperation systemOperation;

    public CryptoProvider createCryptoProvider() {
        final var cryptoKey = authenticate()
                .orElse(this::authenticate)
                .orElse(this::authenticate)
                .orElse(this::authenticationFailed);
        return cryptoKey.isSuccess()
                ? new Cipherizer(cryptoKey.get().getSecret(), cryptoKey.get().getIv())
                : null;
    }

    private Try<Key> authenticate() {
        return keyStoreAdapterPort.loadKey(
                receiveLogin(),
                systemOperation.getPath(configuration.getAdapter().getKeyStore().getLocation())
                        .map(path -> path.resolve(KEYSTORE_FILENAME))
                        .getOrNull());
    }

    private Chars receiveLogin() {
        return userInterfaceAdapterPort
                .receiveSecurely(Output.of(Bytes.of("Enter key: ")))
                .onFailure(failureCollector::collectInputFailure)
                .getOrElse(Input.empty())
                .getBytes()
                .toChars();
    }

    private Try<Key> authenticationFailed() {
        userInterfaceAdapterPort.send(Output.of(Bytes.of("Login failed. Shutting down.")));
        application.terminate(new SystemOperation());
        return Try.failure(new LoginException());
    }

}
