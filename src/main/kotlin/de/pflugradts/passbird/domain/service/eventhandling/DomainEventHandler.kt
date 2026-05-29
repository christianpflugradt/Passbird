package de.pflugradts.passbird.domain.service.eventhandling

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton

@Singleton
class DomainEventHandler @Inject constructor(private val eggRepositoryProvider: Provider<EggRepository>) : EventHandler {
    private val eggRepository: EggRepository get() = eggRepositoryProvider.get()

    @Subscribe
    private fun handleEggDiscarded(eggDiscarded: EggDiscarded) {
        eggRepository.delete(eggDiscarded.egg)
    }
}
