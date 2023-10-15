package de.pflugradts.passbird.domain.service.eventhandling

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.event.PasswordEntryDiscarded
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository

@Singleton
class DomainEventHandler @Inject constructor(
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
) : EventHandler {
    @Subscribe
    private fun handlePasswordEntryDiscarded(passwordEntryDiscarded: PasswordEntryDiscarded) {
        passwordEntryRepository.delete(passwordEntryDiscarded.passwordEntry)
    }
}
