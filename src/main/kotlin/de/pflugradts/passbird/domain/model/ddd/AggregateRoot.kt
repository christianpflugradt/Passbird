package de.pflugradts.passbird.domain.model.ddd

import java.util.Collections

/**
 * Denotes a Domain Entity that serves as an AggregateRoot in Domain-driven Design.
 */
abstract class AggregateRoot {
    private val domainEvents = mutableListOf<DomainEvent>()
    fun registerDomainEvent(domainEvent: DomainEvent) { domainEvents.add(domainEvent) }
    fun clearDomainEvents() { domainEvents.clear() }
    fun getDomainEvents(): List<DomainEvent> = Collections.unmodifiableList(domainEvents)
}
