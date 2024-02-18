package de.pflugradts.passbird.domain.service.eventhandling

import de.pflugradts.kotlinextensions.GuiceTestProvider
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class DomainEventHandlerTest {

    private val eggRepository = mockk<EggRepository>()
    private val domainEventHandler = DomainEventHandler(GuiceTestProvider(eggRepository))
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
}
