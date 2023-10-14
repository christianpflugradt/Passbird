package de.pflugradts.passbird.application.eventhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.domain.model.PasswordEntryCreated;
import de.pflugradts.passbird.domain.model.PasswordEntryDiscarded;
import de.pflugradts.passbird.domain.model.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.PasswordEntryRenamed;
import de.pflugradts.passbird.domain.model.PasswordEntryUpdated;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static de.pflugradts.passbird.domain.model.password.PasswordEntryTestFactoryKt.createPasswordEntryForTesting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ApplicationEventHandlerTestIT {

    private PassbirdEventRegistry passbirdEventRegistry;

    @Mock
    private CryptoProvider cryptoProvider;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private ApplicationEventHandler applicationEventHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    void setup() {
        passbirdEventRegistry = new PassbirdEventRegistry(Set.of(applicationEventHandler), null);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldProcessPasswordEntryCreated() {
        // given
        final var givenPasswordEntry = setupPasswordEntry();
        final var passwordEntryCreated = new PasswordEntryCreated(givenPasswordEntry);
        final var expectedBytes = Bytes.bytesOf("expected key");
        given(cryptoProvider.decrypt(givenPasswordEntry.viewKey())).willReturn(expectedBytes);

        // when
        passbirdEventRegistry.register(passwordEntryCreated);
        passbirdEventRegistry.processEvents();

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains(expectedBytes.asString());
    }

    @Test
    void shouldProcessPasswordEntryUpdated() {
        // given
        final var givenPasswordEntry = setupPasswordEntry();
        final var passwordEntryUpdated = new PasswordEntryUpdated(givenPasswordEntry);
        final var expectedBytes = Bytes.bytesOf("expected key");
        given(cryptoProvider.decrypt(givenPasswordEntry.viewKey())).willReturn(expectedBytes);

        // when
        passbirdEventRegistry.register(passwordEntryUpdated);
        passbirdEventRegistry.processEvents();

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains(expectedBytes.asString());
    }

    @Test
    void shouldProcessPasswordEntryRenamed() {
        // given
        final var givenPasswordEntry = setupPasswordEntry();
        final var passwordEntryRenamed = new PasswordEntryRenamed(givenPasswordEntry);
        final var expectedBytes = Bytes.bytesOf("expected key");
        given(cryptoProvider.decrypt(givenPasswordEntry.viewKey())).willReturn(expectedBytes);

        // when
        passbirdEventRegistry.register(passwordEntryRenamed);
        passbirdEventRegistry.processEvents();

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains(expectedBytes.asString());
    }

    @Test
    void shouldProcessPasswordEntryDiscarded() {
        // given
        final var givenPasswordEntry = setupPasswordEntry();
        final var passwordEntryDiscarded = new PasswordEntryDiscarded(givenPasswordEntry);
        final var expectedBytes = Bytes.bytesOf("expected key");
        given(cryptoProvider.decrypt(givenPasswordEntry.viewKey())).willReturn(expectedBytes);

        // when
        passbirdEventRegistry.register(passwordEntryDiscarded);
        passbirdEventRegistry.processEvents();

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains(expectedBytes.asString());
    }

    @Test
    void shouldProcessPasswordEntryNotFound() {
        // given
        final var givenKey = Bytes.bytesOf("given key");
        final var passwordEntryNotFound = new PasswordEntryNotFound(givenKey);
        final var expectedBytes = Bytes.bytesOf("expected key");
        given(cryptoProvider.decrypt(givenKey)).willReturn(expectedBytes);

        // when
        passbirdEventRegistry.register(passwordEntryNotFound);
        passbirdEventRegistry.processEvents();

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains(expectedBytes.asString());
    }

    private PasswordEntry setupPasswordEntry() {
        return createPasswordEntryForTesting(Bytes.bytesOf("foo"), Bytes.bytesOf("foo"), NamespaceSlot.DEFAULT);
    }

}
