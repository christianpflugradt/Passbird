package de.pflugradts.pwman3.adapter.passwordstore;

import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.control.Try;
import java.io.File;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PasswordFileStoreTestIT {

    private FailureCollector failureCollector = mock(FailureCollector.class);
    private Configuration configuration = mock(Configuration.class);
    private CryptoProvider cryptoProvider = mock(CryptoProvider.class);
    private PasswordFileStore passwordFileStore = setupPasswordFileStore();

    private String tempPasswordStoreDirectory;
    private String keyStoreFile;

    @BeforeEach
    private void setup() {
        setupFileSystem();
        setupMocks();
    }

    private void setupFileSystem() {
        tempPasswordStoreDirectory = UUID.randomUUID().toString();
        keyStoreFile = tempPasswordStoreDirectory + File.separator + ReadableConfiguration.DATABASE_FILENAME;
        assertThat(new File(tempPasswordStoreDirectory).mkdir()).isTrue();
    }

    private void setupMocks() {
        given(cryptoProvider.encrypt(any(Bytes.class))).willAnswer(
                invocation -> Try.of(() -> invocation.getArgument(0)));
        given(cryptoProvider.decrypt(any(Bytes.class))).willAnswer(
                invocation -> Try.of(() -> invocation.getArgument(0)));
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withPasswordStoreLocation(tempPasswordStoreDirectory).fake();
    }

    @AfterEach
    private void cleanup() {
        assertThat(new File(keyStoreFile).delete()).isTrue();
        assertThat(new File(tempPasswordStoreDirectory).delete()).isTrue();
    }

    @Test
    void shouldUsePasswordDatabase_Roundtrip() {
        // given
        final var passwordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("key1"))
                .withPasswordBytes(Bytes.of("password1")).fake();
        final var passwordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("key2"))
                .withPasswordBytes(Bytes.of("password2")).fake();
        final var passwordEntry3 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("key3"))
                .withPasswordBytes(Bytes.of("password3")).fake();

        // when
        passwordFileStore.sync(() -> Stream.of(passwordEntry1, passwordEntry2, passwordEntry3));
        final var actual = passwordFileStore.restore();

        // then
        assertThat(actual).isNotNull().extracting(Supplier::get).isNotNull();
        assertThat(actual.get().collect(Collectors.toList())).isNotNull()
                .containsExactlyInAnyOrder(passwordEntry1, passwordEntry2, passwordEntry3);
    }

    @Test
    void shouldUseEmptyPasswordDatabase_Roundtrip() {
        // when / then
        passwordFileStore.sync(Stream::empty);
        final var actual = passwordFileStore.restore();

        // then
        assertThat(actual).isNotNull().extracting(Supplier::get).isNotNull();
        assertThat(actual.get().collect(Collectors.toList())).isEmpty();
    }

    private PasswordFileStore setupPasswordFileStore() {
        return new PasswordFileStore(
                new SystemOperation(),
                failureCollector,
                configuration,
                new PasswordEntryTransformer(),
                cryptoProvider);
    }

}
