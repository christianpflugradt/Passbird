package de.pflugradts.pwman3.domain.model.ddd;

import java.util.ArrayList;
import java.util.List;
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
            private final List<DomainEvent> domainEventList = new ArrayList<>();
            @Override
            public List<DomainEvent> getDomainEvents() {
                return domainEventList;
            }
        };
    }

}
