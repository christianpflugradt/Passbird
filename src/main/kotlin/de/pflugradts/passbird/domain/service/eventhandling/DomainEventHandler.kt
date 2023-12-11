package de.pflugradts.passbird.domain.service.eventhandling

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.service.password.storage.EggRepository

@Singleton
class DomainEventHandler @Inject constructor(
    @Inject private val eggRepository: EggRepository,
) : EventHandler {
    @Subscribe
    private fun handleEggDiscarded(eggDiscarded: EggDiscarded) {
        eggRepository.delete(eggDiscarded.egg)
    }
}
