package de.pflugradts.passbird.application.eventhandling

import com.google.common.eventbus.EventBus
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggUpdated
import io.mockk.Called
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty

class PassbirdEventRegistryTest {
    private val eventBus = mockk<EventBus>(relaxed = true)
    private val passbirdEventRegistry = spyk(PassbirdEventRegistry(emptySet(), eventBus))

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
        verify(exactly = 1) { eventBus.post(domainEvent1) }
        verify(exactly = 1) { eventBus.post(domainEvent2) }
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
        verify(exactly = 1) { eventBus.post(domainEvent1) }
        verify(exactly = 1) { eventBus.post(domainEvent2) }
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
        verify { eventBus wasNot Called }
        expectThat(aggregate.getDomainEvents()).contains(domainEvent1)
    }
}
