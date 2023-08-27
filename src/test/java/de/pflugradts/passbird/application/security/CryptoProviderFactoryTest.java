package de.pflugradts.passbird.application.security;

import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.configuration.ConfigurationFaker;
import de.pflugradts.passbird.application.configuration.Configuration;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.application.util.PathFaker;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.application.util.SystemOperationFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import io.vavr.control.Try;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CryptoProviderFactoryTest {

    @Mock
    private Bootable application;
    @Mock
    private FailureCollector failureCollector;
    @Mock
    private Configuration configuration;
    @Mock
    private KeyStoreAdapterPort keyStoreAdapterPort;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private SystemOperation systemOperation;
    @InjectMocks
    private CryptoProviderFactory cryptoProviderFactory;

    @Test
    void shouldCreateCryptoProvider() {
        // given
        final var correctPassword = Input.of(Bytes.of("letmein"));
        final var keyStoreDirectory = "tmp";
        final var keyStoreFilePath = PathFaker.faker().fakePath().fake();
        final var keyStoreDirPath = PathFaker.faker()
                .fakePath()
                .withPathResolvingTo(keyStoreFilePath, ReadableConfiguration.KEYSTORE_FILENAME).fake();
        SystemOperationFaker.faker()
                .forInstance(systemOperation)
                .withPath(keyStoreDirectory, keyStoreDirPath).fake();
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation(keyStoreDirectory).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(correctPassword).fake();
        givenLoginSucceeds(correctPassword, keyStoreFilePath);

        // when
        final var actual = cryptoProviderFactory.createCryptoProvider();

        // then
        assertThat(actual).isNotNull().isInstanceOf(Cipherizer.class);
    }

    @Test
    void shouldCreateCryptoProvider_On3rdPasswordInputAttempt() {
        // given
        final var incorrectPassword = Input.of(Bytes.of("letmeout"));
        final var correctPassword = Input.of(Bytes.of("letmein"));
        final var keyStoreDirectory = "tmp";
        final var keyStoreFilePath = PathFaker.faker().fakePath().fake();
        final var keyStoreDirPath = PathFaker.faker()
                .fakePath()
                .withPathResolvingTo(keyStoreFilePath, ReadableConfiguration.KEYSTORE_FILENAME).fake();
        SystemOperationFaker.faker()
                .forInstance(systemOperation)
                .withPath(keyStoreDirectory, keyStoreDirPath).fake();
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation(keyStoreDirectory).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(incorrectPassword, incorrectPassword, correctPassword).fake();
        givenLoginFails(incorrectPassword, keyStoreFilePath);
        givenLoginSucceeds(correctPassword, keyStoreFilePath);

        // when
        final var actual = cryptoProviderFactory.createCryptoProvider();

        // then
        assertThat(actual).isNotNull().isInstanceOf(Cipherizer.class);
    }

    @Test
    void shouldCreateCryptoProvider_TerminateApplicationAfter3FailedAttempts() {
        // given
        final var incorrectPassword = Input.of(Bytes.of("letmeout"));
        final var keyStoreDirectory = "tmp";
        final var keyStoreFilePath = PathFaker.faker().fakePath().fake();
        final var keyStoreDirPath = PathFaker.faker()
                .fakePath()
                .withPathResolvingTo(keyStoreFilePath, ReadableConfiguration.KEYSTORE_FILENAME).fake();
        SystemOperationFaker.faker()
                .forInstance(systemOperation)
                .withPath(keyStoreDirectory, keyStoreDirPath).fake();
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation(keyStoreDirectory).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(incorrectPassword, incorrectPassword, incorrectPassword).fake();
        givenLoginFails(incorrectPassword, keyStoreFilePath);

        // when
        cryptoProviderFactory.createCryptoProvider();

        // then
        then(application).should().terminate(any(SystemOperation.class));
    }

    private void givenLoginSucceeds(final Input password, final Path keyStoreFilePath) {
        given(keyStoreAdapterPort.loadKey(eq(password.getBytes().toChars()), eq(keyStoreFilePath)))
                .willReturn(Try.of(() -> new Key(Bytes.empty(), Bytes.empty())));
    }

    private void givenLoginFails(final Input password, final Path keyStoreFilePath) {
        given(keyStoreAdapterPort.loadKey(eq(password.getBytes().toChars()), eq(keyStoreFilePath)))
                .willReturn(Try.failure(new RuntimeException()));
    }

}
