package de.pflugradts.passbird.domain.service.eventhandling

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.ddd.DomainEvent

interface EventRegistry {
    fun register(aggregateRoot: AggregateRoot)
    fun register(domainEvent: DomainEvent)
    fun deregister(aggregateRoot: AggregateRoot)
    fun processEvents()
}
