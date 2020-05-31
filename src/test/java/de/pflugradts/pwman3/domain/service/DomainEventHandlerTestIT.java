package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.security.CryptoProvider;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryCreated;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryUpdated;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import io.vavr.control.Try;
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
class DomainEventHandlerTestIT {

    private DomainEventRegistry domainEventRegistry;

    @Mock
    private CryptoProvider cryptoProvider;
    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private DomainEventHandler domainEventHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    private void setup() {
        domainEventRegistry = new DomainEventRegistry(domainEventHandler, null);
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
        domainEventRegistry.register(passwordEntryCreated);
        domainEventRegistry.processEvents();

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
        domainEventRegistry.register(passwordEntryUpdated);
        domainEventRegistry.processEvents();

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
        domainEventRegistry.register(passwordEntryDiscarded);
        domainEventRegistry.processEvents();

        // then
        then(passwordEntryRepository).should().delete(giverPasswordEntry);
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
        domainEventRegistry.register(passwordEntryNotFound);
        domainEventRegistry.processEvents();

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains(expectedBytes.asString());
    }

}
