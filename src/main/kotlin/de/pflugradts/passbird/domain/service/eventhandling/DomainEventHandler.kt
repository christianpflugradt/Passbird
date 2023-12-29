package de.pflugradts.passbird.domain.service.eventhandling

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.NestCreated
import de.pflugradts.passbird.domain.model.event.NestDiscarded
import de.pflugradts.passbird.domain.service.password.storage.EggRepository

@Singleton
class DomainEventHandler @Inject constructor(
    @Inject private val eggRepositoryProvider: Provider<EggRepository>,
) : EventHandler {
    private val eggRepository: EggRepository get() = eggRepositoryProvider.get()

    @Subscribe
    private fun handleEggDiscarded(eggDiscarded: EggDiscarded) {
        eggRepository.delete(eggDiscarded.egg)
    }

    @Subscribe
    private fun handleNestCreated(@Suppress("UNUSED_PARAMETER") nestCreated: NestCreated) {
        eggRepository.sync()
    }

    @Subscribe
    private fun handleNestDiscarded(@Suppress("UNUSED_PARAMETER") nestDiscarded: NestDiscarded) {
        eggRepository.sync()
    }
}
