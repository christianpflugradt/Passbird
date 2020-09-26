package de.pflugradts.pwman3.application.eventhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.domain.model.event.*;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ApplicationEventHandlerTestIT {

    private PwMan3EventRegistry pwMan3EventRegistry;

    @Mock
    private CryptoProvider cryptoProvider;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private ApplicationEventHandler applicationEventHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    private void setup() {
        pwMan3EventRegistry = new PwMan3EventRegistry(Set.of(applicationEventHandler), null);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldProcessPasswordEntryCreated() {
        // given
        final var givenPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryCreated = new PasswordEntryCreated(givenPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");
        given(cryptoProvider.decrypt(givenPasswordEntry.viewKey())).willReturn(Try.success(expectedBytes));

        // when
        pwMan3EventRegistry.register(passwordEntryCreated);
        pwMan3EventRegistry.processEvents();

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
        final var givenPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryUpdated = new PasswordEntryUpdated(givenPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");
        given(cryptoProvider.decrypt(givenPasswordEntry.viewKey())).willReturn(Try.success(expectedBytes));

        // when
        pwMan3EventRegistry.register(passwordEntryUpdated);
        pwMan3EventRegistry.processEvents();

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
        final var givenPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryRenamed = new PasswordEntryRenamed(givenPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");
        given(cryptoProvider.decrypt(givenPasswordEntry.viewKey())).willReturn(Try.success(expectedBytes));

        // when
        pwMan3EventRegistry.register(passwordEntryRenamed);
        pwMan3EventRegistry.processEvents();

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
        final var givenPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryDiscarded = new PasswordEntryDiscarded(givenPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");
        given(cryptoProvider.decrypt(givenPasswordEntry.viewKey())).willReturn(Try.success(expectedBytes));

        // when
        pwMan3EventRegistry.register(passwordEntryDiscarded);
        pwMan3EventRegistry.processEvents();

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
        final var givenKey = Bytes.of("given key");
        final var passwordEntryNotFound = new PasswordEntryNotFound(givenKey);
        final var expectedBytes = Bytes.of("expected key");
        given(cryptoProvider.decrypt(givenKey)).willReturn(Try.success(expectedBytes));

        // when
        pwMan3EventRegistry.register(passwordEntryNotFound);
        pwMan3EventRegistry.processEvents();

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains(expectedBytes.asString());
    }

}
