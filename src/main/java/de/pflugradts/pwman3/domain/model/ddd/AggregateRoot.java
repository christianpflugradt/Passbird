package de.pflugradts.pwman3.domain.model.ddd;

import java.util.List;

/**
 * Denotes a Domain Entity that serves as an AggregateRoot in Domain-driven Design.
 */
public interface AggregateRoot {
    List<DomainEvent> getDomainEvents();
    default void registerDomainEvent(final DomainEvent domainEvent) {
        getDomainEvents().add(domainEvent);
    }
    default void clearDomainEvents() {
        getDomainEvents().clear();
    }
}
