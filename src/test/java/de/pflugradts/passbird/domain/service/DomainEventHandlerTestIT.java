package de.pflugradts.passbird.domain.service;

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry;
import de.pflugradts.passbird.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.eventhandling.DomainEventHandler;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DomainEventHandlerTestIT {

    private PassbirdEventRegistry passbirdEventRegistry;

    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @InjectMocks
    private DomainEventHandler domainEventHandler;

    @BeforeEach
    void setup() {
        passbirdEventRegistry = new PassbirdEventRegistry(Set.of(domainEventHandler), null);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldProcessPasswordEntryDiscarded() {
        // given
        final var giverPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryDiscarded = new PasswordEntryDiscarded(giverPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");

        // when
        passbirdEventRegistry.register(passwordEntryDiscarded);
        passbirdEventRegistry.processEvents();

        // then
        then(passwordEntryRepository).should().delete(giverPasswordEntry);
    }

}
