package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.application.eventhandling.PwMan3EventRegistry;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.eventhandling.DomainEventHandler;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
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

    private PwMan3EventRegistry pwMan3EventRegistry;

    @Mock
    private PasswordEntryRepository passwordEntryRepository;
    @InjectMocks
    private DomainEventHandler domainEventHandler;

    @BeforeEach
    void setup() {
        pwMan3EventRegistry = new PwMan3EventRegistry(Set.of(domainEventHandler), null);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldProcessPasswordEntryDiscarded() {
        // given
        final var giverPasswordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var passwordEntryDiscarded = new PasswordEntryDiscarded(giverPasswordEntry);
        final var expectedBytes = Bytes.of("expected key");

        // when
        pwMan3EventRegistry.register(passwordEntryDiscarded);
        pwMan3EventRegistry.processEvents();

        // then
        then(passwordEntryRepository).should().delete(giverPasswordEntry);
    }

}
