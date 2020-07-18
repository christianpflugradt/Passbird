package de.pflugradts.pwman3.domain.service;

import com.google.common.eventbus.EventBus;
import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryCreated;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryUpdated;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.application.eventhandling.PwMan3EventRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PwMan3EventRegistryTest {

    @Mock
    private EventBus eventBus;
    @InjectMocks
    private PwMan3EventRegistry pwMan3EventRegistry;

    @Test
    void shouldProcessDomainEvents() {
        // given
        final var domainEvent1 = mock(DomainEvent.class);
        final var domainEvent2 = mock(DomainEvent.class);

        // when
        pwMan3EventRegistry.register(domainEvent1);
        pwMan3EventRegistry.register(domainEvent2);
        pwMan3EventRegistry.processEvents();

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
        pwMan3EventRegistry.register(aggregate);
        pwMan3EventRegistry.processEvents();

        // then
        then(eventBus).should().post(domainEvent1);
        then(eventBus).should().post(domainEvent2);
        assertThat(aggregate.getDomainEvents()).isNotNull().isEmpty();
    }

    @Test
    void shouldDeregisterAggregate() {
        // given
        final var aggregate = PasswordEntryFaker.faker().fakePasswordEntry().fake();
        final var domainEvent1 = new PasswordEntryCreated(aggregate);
        aggregate.registerDomainEvent(domainEvent1);
        pwMan3EventRegistry.register(aggregate);

        // when
        pwMan3EventRegistry.deregister(aggregate);
        pwMan3EventRegistry.processEvents();

        // then
        then(eventBus).shouldHaveNoInteractions();
        assertThat(aggregate.getDomainEvents()).contains(domainEvent1);
    }

}
