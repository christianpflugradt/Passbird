package de.pflugradts.passbird.application.eventhandling

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap
import java.util.Queue
import java.util.logging.Level
import java.util.logging.Logger

class PassbirdEventRegistry constructor(
    eventHandlers: Set<EventHandler>,
) : EventRegistry {
    private val eventHandlers = eventHandlers.toList()
    private val aggregateRoots: MutableSet<AggregateRoot> = Collections.newSetFromMap(IdentityHashMap())
    private val domainEvents: Queue<DomainEvent> = ArrayDeque()
    private val abandonedAggregateRoots: Queue<AggregateRoot> = ArrayDeque()

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

    override fun clearEvents() {
        processAbandonedAggregateRoots()
        aggregateRoots.forEach { it.clearDomainEvents() }
        domainEvents.clear()
        abandonedAggregateRoots.clear()
    }

    private fun processAggregateRoots() {
        aggregateRoots.forEach { aggregateRoot ->
            aggregateRoot.getDomainEvents().forEach(::postEvent)
            aggregateRoot.clearDomainEvents()
        }
    }

    private fun processDomainEvents() {
        while (!domainEvents.isEmpty()) {
            val domainEvent = domainEvents.peek()
            postEvent(domainEvent)
            domainEvents.remove()
        }
    }

    private fun postEvent(domainEvent: DomainEvent) {
        var subscriberException: RuntimeException? = null
        eventHandlers
            .filter { eventHandler -> eventHandler.eventTypes.any { eventType -> eventType.isAssignableFrom(domainEvent.javaClass) } }
            .forEach { eventHandler ->
                subscriberException = eventHandler.handleCatching(domainEvent, subscriberException)
            }
        subscriberException?.let { throw it }
    }

    private fun EventHandler.handleCatching(domainEvent: DomainEvent, previousException: RuntimeException?) =
        runCatching { handle(domainEvent) }
            .fold(
                onSuccess = { previousException },
                onFailure = { exception ->
                    exception.throwIfFatal()
                    LOGGER.log(Level.SEVERE, "Exception thrown by event handler", exception)
                    previousException ?: exception.asRuntimeException()
                },
            )

    private fun processAbandonedAggregateRoots() {
        while (!abandonedAggregateRoots.isEmpty()) aggregateRoots.remove(abandonedAggregateRoots.poll())
    }

    private companion object {
        val LOGGER: Logger = Logger.getLogger(PassbirdEventRegistry::class.java.name)
    }
}

private fun Throwable.asRuntimeException() = this as? RuntimeException ?: RuntimeException(this)

private fun Throwable.throwIfFatal() {
    if (this is VirtualMachineError || this is ThreadDeath || this is LinkageError) throw this
}
