package de.pflugradts.passbird.application.eventhandling

import com.google.common.eventbus.EventBus
import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import java.util.ArrayDeque
import java.util.Queue

@Singleton
class PassbirdEventRegistry @Inject constructor(
    eventHandlers: Set<EventHandler>,
    private val eventBus: EventBus = EventBus(),
) : EventRegistry {
    private val aggregateRoots = mutableSetOf<AggregateRoot>()
    private val domainEvents: Queue<DomainEvent> = ArrayDeque()
    private val abandonedAggregateRoots: Queue<AggregateRoot> = ArrayDeque()

    init {
        eventHandlers.forEach { eventBus.register(it) }
    }
    override fun register(aggregateRoot: AggregateRoot) {
        aggregateRoots.add(aggregateRoot)
    }
    override fun register(domainEvent: DomainEvent) {
        domainEvents.add(domainEvent)
    }
    override fun deregister(aggregateRoot: AggregateRoot) {
        abandonedAggregateRoots.add(aggregateRoot)
    }

    override fun processEvents() {
        processAbandonedAggregateRoots()
        processAggregateRoots()
        processDomainEvents()
        processAbandonedAggregateRoots()
    }

    private fun processAggregateRoots() {
        aggregateRoots.forEach { aggregateRoot ->
            aggregateRoot.getDomainEvents().forEach { eventBus.post(it) }
            aggregateRoot.clearDomainEvents()
        }
    }

    private fun processDomainEvents() {
        while (!domainEvents.isEmpty()) {
            eventBus.post(domainEvents.poll())
        }
    }

    private fun processAbandonedAggregateRoots() {
        while (!abandonedAggregateRoots.isEmpty()) aggregateRoots.remove(abandonedAggregateRoots.poll())
    }
}
