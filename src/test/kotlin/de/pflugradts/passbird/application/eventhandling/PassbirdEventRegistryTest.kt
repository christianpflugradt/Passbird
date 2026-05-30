package de.pflugradts.passbird.application.eventhandling

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

class PassbirdEventRegistryTest {
    private val collectedEvents = mutableListOf<DomainEvent>()
    private val passbirdEventRegistry = PassbirdEventRegistry(setOf(CollectingEventHandler(collectedEvents)))

    @Test
    fun `should process domain events`() {
        // given
        val domainEvent1 = mockk<DomainEvent>()
        val domainEvent2 = mockk<DomainEvent>()

        // when
        passbirdEventRegistry.register(domainEvent1)
        passbirdEventRegistry.register(domainEvent2)
        passbirdEventRegistry.processEvents()

        // then
        expectThat(collectedEvents).containsExactly(domainEvent1, domainEvent2)
    }

    @Test
    fun `should process and clear all aggregate events`() {
        // given
        val aggregate = createEggForTesting()
        aggregate.clearDomainEvents()
        val domainEvent1 = EggCreated(aggregate)
        val domainEvent2 = EggUpdated(aggregate)
        aggregate.registerDomainEvent(domainEvent1)
        aggregate.registerDomainEvent(domainEvent2)

        // when
        passbirdEventRegistry.register(aggregate)
        passbirdEventRegistry.processEvents()

        // then
        expectThat(collectedEvents).containsExactly(domainEvent1, domainEvent2)
        expectThat(aggregate.getDomainEvents()).isEmpty()
    }

    @Test
    fun `should deregister aggregate`() {
        // given
        val aggregate = createEggForTesting()
        val domainEvent1 = EggCreated(aggregate)
        aggregate.registerDomainEvent(domainEvent1)
        passbirdEventRegistry.register(aggregate)

        // when
        passbirdEventRegistry.deregister(aggregate)
        passbirdEventRegistry.processEvents()

        // then
        expectThat(collectedEvents).isEmpty()
        expectThat(aggregate.getDomainEvents()).contains(domainEvent1)
    }

    @Test
    fun `should clear queued aggregate and domain events`() {
        // given
        val aggregate = createEggForTesting()
        val aggregateEvent = EggCreated(aggregate)
        val queuedEvent = mockk<DomainEvent>()
        aggregate.registerDomainEvent(aggregateEvent)
        passbirdEventRegistry.register(aggregate)
        passbirdEventRegistry.register(queuedEvent)

        // when
        passbirdEventRegistry.clearEvents()
        passbirdEventRegistry.processEvents()

        // then
        expectThat(collectedEvents).isEmpty()
        expectThat(aggregate.getDomainEvents()).isEmpty()
    }

    @Test
    fun `should propagate subscriber exceptions and leave queued domain event available for retry`() {
        // given
        val collectedEvents = mutableListOf<DomainEvent>()
        val eventHandler = FailingOnceEventHandler(collectedEvents)
        val eventRegistry = PassbirdEventRegistry(setOf(eventHandler))
        val domainEvent = mockk<DomainEvent>()
        eventRegistry.register(domainEvent)

        // when / then
        assertThrows<IllegalStateException> { eventRegistry.processEvents() }
        eventRegistry.processEvents()
        expectThat(collectedEvents).containsExactly(domainEvent)
    }

    private class CollectingEventHandler(
        private val events: MutableList<DomainEvent>,
    ) : EventHandler {
        @Subscribe
        private fun handle(domainEvent: DomainEvent) {
            events.add(domainEvent)
        }
    }

    private class FailingOnceEventHandler(
        private val events: MutableList<DomainEvent>,
    ) : EventHandler {
        private var shouldFail = true

        @Subscribe
        private fun handle(domainEvent: DomainEvent) {
            if (shouldFail) {
                shouldFail = false
                throw IllegalStateException("event failed")
            }
            events.add(domainEvent)
        }
    }
}
