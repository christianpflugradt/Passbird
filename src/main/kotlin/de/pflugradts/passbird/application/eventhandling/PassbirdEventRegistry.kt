package de.pflugradts.passbird.application.eventhandling

import com.google.common.eventbus.EventBus
import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap
import java.util.Queue
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import java.util.logging.Logger

@Singleton
class PassbirdEventRegistry @Inject constructor(
    eventHandlers: Set<EventHandler>,
) : EventRegistry {
    private val subscriberException = AtomicReference<RuntimeException?>()
    private val eventBus = EventBus { exception, _ ->
        LOGGER.log(Level.SEVERE, "Exception thrown by event handler", exception)
        subscriberException.compareAndSet(null, exception.asRuntimeException())
    }
    private val aggregateRoots: MutableSet<AggregateRoot> = Collections.newSetFromMap(IdentityHashMap())
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
        subscriberException.set(null)
        eventBus.post(domainEvent)
        subscriberException.getAndSet(null)?.let { throw it }
    }

    private fun processAbandonedAggregateRoots() {
        while (!abandonedAggregateRoots.isEmpty()) aggregateRoots.remove(abandonedAggregateRoots.poll())
    }

    private companion object {
        val LOGGER: Logger = Logger.getLogger(PassbirdEventRegistry::class.java.name)
    }
}

private fun Throwable.asRuntimeException() = this as? RuntimeException ?: RuntimeException(this)
