package de.pflugradts.passbird.domain.service.eventhandling

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.service.password.tree.EggRepository

class DomainEventHandler(private val eggRepositoryProvider: () -> EggRepository) : EventHandler {
    private val eggRepository: EggRepository get() = eggRepositoryProvider()

    @Subscribe
    private fun handleEggDiscarded(eggDiscarded: EggDiscarded) {
        eggRepository.delete(eggDiscarded.egg)
    }
}
