package de.pflugradts.passbird.domain.service;

import com.google.common.eventbus.EventBus;
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry;
import de.pflugradts.passbird.domain.model.PasswordEntryCreated;
import de.pflugradts.passbird.domain.model.PasswordEntryUpdated;
import de.pflugradts.passbird.domain.model.ddd.DomainEvent;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PassbirdEventRegistryTest {

    @Mock
    private EventBus eventBus;
    @InjectMocks
    private PassbirdEventRegistry passbirdEventRegistry;

    @Test
    void shouldProcessDomainEvents() {
        // given
        final var domainEvent1 = mock(DomainEvent.class);
        final var domainEvent2 = mock(DomainEvent.class);

        // when
        passbirdEventRegistry.register(domainEvent1);
        passbirdEventRegistry.register(domainEvent2);
        passbirdEventRegistry.processEvents();

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
        passbirdEventRegistry.register(aggregate);
        passbirdEventRegistry.processEvents();

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
        passbirdEventRegistry.register(aggregate);

        // when
        passbirdEventRegistry.deregister(aggregate);
        passbirdEventRegistry.processEvents();

        // then
        then(eventBus).shouldHaveNoInteractions();
        assertThat(aggregate.getDomainEvents()).contains(domainEvent1);
    }

}
