package de.pflugradts.passbird.application.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Chars;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import io.vavr.control.Try;
import javax.security.auth.login.LoginException;
import static de.pflugradts.passbird.application.configuration.ReadableConfiguration.KEYSTORE_FILENAME;

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
