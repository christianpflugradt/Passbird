package de.pflugradts.passbird.domain.service.eventhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.NestDiscarded
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import io.mockk.mockk
import io.mockk.verify
import jakarta.inject.Provider
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class DomainEventHandlerTest {

    private val eggRepository = mockk<EggRepository>(relaxed = true)
    private val domainEventHandler = DomainEventHandler(Provider { eggRepository })
    private var passbirdEventRegistry = PassbirdEventRegistry(mutableSetOf<EventHandler>(domainEventHandler))

    @Test
    fun `should process egg discarded`() {
        // given
        val giverEgg = createEggForTesting()
        val eggDiscarded = EggDiscarded(giverEgg)

        // when
        passbirdEventRegistry.register(eggDiscarded)
        passbirdEventRegistry.processEvents()

        // then
        verify(exactly = 1) { eggRepository.delete(giverEgg) }
    }

    @Test
    fun `should process nest discarded`() {
        // given
        val givenNest = createNest(shellOf("nest"), S3)
        val nestDiscarded = NestDiscarded(givenNest)

        // when
        passbirdEventRegistry.register(nestDiscarded)
        passbirdEventRegistry.processEvents()

        // then
        verify(exactly = 1) { eggRepository.discardFavorites(S3) }
        verify(exactly = 1) { eggRepository.sync() }
    }
}
