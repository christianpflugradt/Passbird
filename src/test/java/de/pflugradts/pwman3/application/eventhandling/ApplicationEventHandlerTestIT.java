package de.pflugradts.pwman3.application.eventhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryCreated;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryUpdated;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import io.vavr.control.Try;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
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
        final var giverPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryCreated = new PasswordEntryCreated(giverPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");
        given(cryptoProvider.decrypt(giverPasswordEntry.viewKey())).willReturn(Try.success(expectedBytes));

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
        final var giverPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryUpdated = new PasswordEntryUpdated(giverPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");
        given(cryptoProvider.decrypt(giverPasswordEntry.viewKey())).willReturn(Try.success(expectedBytes));

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
    void shouldProcessPasswordEntryDiscarded() {
        // given
        final var giverPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryDiscarded = new PasswordEntryDiscarded(giverPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");
        given(cryptoProvider.decrypt(giverPasswordEntry.viewKey())).willReturn(Try.success(expectedBytes));

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
