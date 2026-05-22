package de.pflugradts.passbird.domain.model.ddd

import java.util.Collections

abstract class AggregateRoot {
    private val domainEvents = mutableListOf<DomainEvent>()
    fun registerDomainEvent(domainEvent: DomainEvent) {
        domainEvents.add(domainEvent)
    }
    fun clearDomainEvents() {
        domainEvents.clear()
    }
    fun getDomainEvents(): List<DomainEvent> = Collections.unmodifiableList(domainEvents)
}
