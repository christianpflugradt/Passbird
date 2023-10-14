package de.pflugradts.passbird.domain.model.ddd

/**
 * Denotes a Domain Entity that serves as an AggregateRoot in Domain-driven Design.
 */
interface AggregateRoot {
    val domainEvents: MutableList<DomainEvent>
    fun registerDomainEvent(domainEvent: DomainEvent) { domainEvents.add(domainEvent) }
    fun clearDomainEvents() { domainEvents.clear() }
}
