package de.pflugradts.passbird.domain.service.eventhandling

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.domain.model.event.PasswordEntryDiscarded
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class DomainEventHandlerIT {

    private val passwordEntryRepository = mockk<PasswordEntryRepository>()
    private val domainEventHandler = DomainEventHandler(passwordEntryRepository)
    private var passbirdEventRegistry = PassbirdEventRegistry(mutableSetOf<EventHandler>(domainEventHandler), null)

    @Test
    fun `should process password entry discarded`() {
        // given
        val giverPasswordEntry = createPasswordEntryForTesting()
        val passwordEntryDiscarded = PasswordEntryDiscarded(giverPasswordEntry)

        // when
        passbirdEventRegistry.register(passwordEntryDiscarded)
        passbirdEventRegistry.processEvents()

        // then
        verify(exactly = 1) { passwordEntryRepository.delete(giverPasswordEntry) }
    }
}
