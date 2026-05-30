package de.pflugradts.passbird.domain.service.eventhandling

import de.pflugradts.passbird.domain.model.ddd.DomainEvent

interface EventHandler {
    val eventTypes: Set<Class<out DomainEvent>>

    fun handle(domainEvent: DomainEvent)
}
