package de.pflugradts.passbird.domain.service.eventhandling;

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot;
import de.pflugradts.passbird.domain.model.ddd.DomainEvent;

public interface EventRegistry {

    void register(AggregateRoot aggregateRoot);

    void register(DomainEvent domainEvent);

    void deregister(AggregateRoot aggregateRoot);

    void processEvents();

}
