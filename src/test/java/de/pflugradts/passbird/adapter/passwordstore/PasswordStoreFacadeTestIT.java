package de.pflugradts.passbird.adapter.passwordstore;

import de.pflugradts.passbird.application.configuration.Configuration;
import de.pflugradts.passbird.application.configuration.MockitoConfigurationFaker;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.namespace.Namespace;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.NamespaceServiceFake;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import io.vavr.control.Try;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._1;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._2;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._3;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._4;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._5;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._6;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._7;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._8;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PasswordStoreFacadeTestIT {

    private final FailureCollector failureCollector = mock(FailureCollector.class);
    private final Configuration configuration = mock(Configuration.class);
    private final CryptoProvider cryptoProvider = mock(CryptoProvider.class);
    private final NamespaceServiceFake namespaceServiceFake = new NamespaceServiceFake();
    private PasswordStoreFacade passwordStoreFacade;

    private String tempPasswordStoreDirectory;
    private String dbFile;

    @BeforeEach
    void setup() {
        setupFileSystem();
        setupMocks();
        passwordStoreFacade = setupPasswordFileStore();
    }

    private void setupFileSystem() {
        tempPasswordStoreDirectory = UUID.randomUUID().toString();
        dbFile = tempPasswordStoreDirectory + File.separator + ReadableConfiguration.DATABASE_FILENAME;
        assertThat(new File(tempPasswordStoreDirectory).mkdir()).isTrue();
    }

    private void setupMocks() {
        given(cryptoProvider.encrypt(any(Bytes.class))).willAnswer(
                invocation -> Try.of(() -> invocation.getArgument(0)));
        given(cryptoProvider.decrypt(any(Bytes.class))).willAnswer(
                invocation -> Try.of(() -> invocation.getArgument(0)));
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withPasswordStoreLocation(tempPasswordStoreDirectory).fake();
    }

    @AfterEach
    void cleanup() {
        assertThat(new File(dbFile).delete()).isTrue();
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
        passwordStoreFacade.sync(() -> Stream.of(passwordEntry1, passwordEntry2, passwordEntry3));
        assertThat(new File(dbFile)).exists();
        final var actual = passwordStoreFacade.restore();

        // then
        assertThat(actual).isNotNull().extracting(Supplier::get).isNotNull();
        assertThat(actual.get().collect(Collectors.toList())).isNotNull()
                .containsExactlyInAnyOrder(passwordEntry1, passwordEntry2, passwordEntry3);
    }

    @Test
    void shouldUsePasswordDatabase_RoundtripWithNamespaces() {
        // given
        final var namespace1 = Bytes.of("namespace1");
        final var namespace3 = Bytes.of("Namespace3");
        final var namespace9 = Bytes.of("+nameSpace*9");
        namespaceServiceFake.deploy(namespace1, _1);
        namespaceServiceFake.deploy(namespace3, _3);
        namespaceServiceFake.deploy(namespace9, _9);
        final var passwordEntry1 = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(Bytes.of("key1"))
            .withPasswordBytes(Bytes.of("password1"))
            .withNamespace(DEFAULT).fake();
        final var passwordEntry2 = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(Bytes.of("key2"))
            .withPasswordBytes(Bytes.of("password2"))
            .withNamespace(_1).fake();
        final var passwordEntry3a = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(Bytes.of("key3"))
            .withPasswordBytes(Bytes.of("password3"))
            .withNamespace(_3).fake();
        final var passwordEntry3b = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(Bytes.of("key3"))
            .withPasswordBytes(Bytes.of("password3b"))
            .withNamespace(_9).fake();

        // when
        passwordStoreFacade.sync(() -> Stream.of(passwordEntry1, passwordEntry2, passwordEntry3a, passwordEntry3b));
        namespaceServiceFake.reset();
        assertThat(new File(dbFile)).exists();
        final var actual = passwordStoreFacade.restore();

        // then
        assertThat(actual).isNotNull().extracting(Supplier::get).isNotNull();
        assertThat(actual.get().collect(Collectors.toList())).isNotNull()
            .containsExactlyInAnyOrder(passwordEntry1, passwordEntry2, passwordEntry3a, passwordEntry3b);
        List.of(_2, _4, _5, _6, _7, _8).forEach(namespace ->
            assertThat(namespaceServiceFake.atSlot(namespace)).isNotPresent());
        assertThat(namespaceServiceFake.atSlot(_1)).isPresent().get()
            .extracting(Namespace::getBytes).isNotNull()
            .isEqualTo(namespace1);
        assertThat(namespaceServiceFake.atSlot(_3)).isPresent().get()
            .extracting(Namespace::getBytes).isNotNull()
            .isEqualTo(namespace3);
        assertThat(namespaceServiceFake.atSlot(_9)).isPresent().get()
            .extracting(Namespace::getBytes).isNotNull()
            .isEqualTo(namespace9);
    }

    @Test
    void shouldUseEmptyPasswordDatabase_Roundtrip() {
        // when / then
        passwordStoreFacade.sync(Stream::empty);
        assertThat(new File(dbFile)).exists();
        final var actual = passwordStoreFacade.restore();

        // then
        assertThat(actual).isNotNull().extracting(Supplier::get).isNotNull();
        assertThat(actual.get().collect(Collectors.toList())).isEmpty();
    }

    @Test
    void shouldCreateEmptyPasswordDatabase_IfFileNotExists() {
        // when / then
        assertThat(new File(dbFile)).doesNotExist();
        final var actual = passwordStoreFacade.restore();

        // then
        assertThat(actual).isNotNull().extracting(Supplier::get).isNotNull();
        assertThat(actual.get().collect(Collectors.toList())).isEmpty();
        passwordStoreFacade.sync(actual); // for cleanup
    }

    @Test
    void shouldCreateEmptyPasswordDatabase_IfFileIsEmpty() throws IOException {
        // when / then
        assertThat(new File(dbFile).createNewFile()).isTrue();
        final var actual = passwordStoreFacade.restore();

        // then
        assertThat(actual).isNotNull().extracting(Supplier::get).isNotNull();
        assertThat(actual.get().collect(Collectors.toList())).isEmpty();
    }

    private PasswordStoreFacade setupPasswordFileStore() {
        final var systemOperation = new SystemOperation();
        final var pTransformer = new PasswordEntryTransformer();
        final var nTransformer = new NamespaceTransformer(namespaceServiceFake);
        final var commons = new PasswordStoreCommons();
        return new PasswordStoreFacade(
            new PasswordStoreReader(systemOperation, failureCollector, configuration, pTransformer, nTransformer, namespaceServiceFake, cryptoProvider, commons),
            new PasswordStoreWriter(systemOperation, failureCollector, configuration, pTransformer, nTransformer, namespaceServiceFake, cryptoProvider, commons));
    }

}
