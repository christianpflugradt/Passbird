package de.pflugradts.passbird.domain.service.eventhandling

import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.service.password.tree.EggRepository

class DomainEventHandler(private val eggRepositoryProvider: () -> EggRepository) : EventHandler {
    override val eventTypes: Set<Class<out DomainEvent>> = setOf(EggDiscarded::class.java)

    private val eggRepository: EggRepository get() = eggRepositoryProvider()

    override fun handle(domainEvent: DomainEvent) {
        when (domainEvent) {
            is EggDiscarded -> handleEggDiscarded(domainEvent)
        }
    }

    private fun handleEggDiscarded(eggDiscarded: EggDiscarded) {
        eggRepository.delete(eggDiscarded.egg)
    }
}
