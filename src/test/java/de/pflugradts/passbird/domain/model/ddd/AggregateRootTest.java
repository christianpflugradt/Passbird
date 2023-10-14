package de.pflugradts.passbird.domain.model.ddd;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateRootTest {

    @Test
    void shouldRegisterDomainEvent() {
        // given
        final var domainEvent = new DomainEvent(){};
        final var aggregate = setupAggregate();

        // when
        aggregate.registerDomainEvent(domainEvent);

        // then
        assertThat(aggregate)
                .extracting(AggregateRoot::getDomainEvents).isNotNull()
                .asList().containsExactly(domainEvent);
    }

    @Test
    void shouldClearDomainEvent() {
        // given
        final var domainEvent = new DomainEvent(){};
        final var aggregate = setupAggregate();

        // when
        aggregate.registerDomainEvent(domainEvent);
        aggregate.clearDomainEvents();

        // then
        assertThat(aggregate)
                .extracting(AggregateRoot::getDomainEvents).isNotNull()
                .asList().isEmpty();
    }

    private AggregateRoot setupAggregate() {
        return new AggregateRoot() {
        };
    }

}
