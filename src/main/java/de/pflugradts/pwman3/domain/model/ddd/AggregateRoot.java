package de.pflugradts.pwman3.domain.model.ddd;

import java.util.List;

public interface AggregateRoot {
    List<DomainEvent> getDomainEvents();
    default void registerDomainEvent(final DomainEvent domainEvent) {
        getDomainEvents().add(domainEvent);
    }
    default void clearDomainEvents() {
        getDomainEvents().clear();
    }
}
