package de.pflugradts.pwman3.domain.service;

import com.google.common.eventbus.EventBus;
import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryCreated;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryUpdated;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DomainEventRegistryTest {

    @Mock
    private EventBus eventBus;
    @InjectMocks
    private DomainEventRegistry domainEventRegistry;

    @Test
    void shouldProcessDomainEvents() {
        // given
        final var domainEvent1 = mock(DomainEvent.class);
        final var domainEvent2 = mock(DomainEvent.class);

        // when
        domainEventRegistry.register(domainEvent1);
        domainEventRegistry.register(domainEvent2);
        domainEventRegistry.processEvents();

        // then
        then(eventBus).should().post(domainEvent1);
        then(eventBus).should().post(domainEvent2);
    }

    @Test
    void shouldProcessAndClearAllAggregateEvents() {
        // given
        final var aggregate = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        aggregate.clearDomainEvents();

        final var domainEvent1 = new PasswordEntryCreated(aggregate);
        final var domainEvent2 = new PasswordEntryUpdated(aggregate);
        aggregate.registerDomainEvent(domainEvent1);
        aggregate.registerDomainEvent(domainEvent2);

        // when
        domainEventRegistry.register(aggregate);
        domainEventRegistry.processEvents();

        // then
        then(eventBus).should().post(domainEvent1);
        then(eventBus).should().post(domainEvent2);
        assertThat(aggregate.getDomainEvents()).isNotNull().isEmpty();
    }

}
