package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import de.pflugradts.passbird.domain.service.password.tree.fakeEggRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class FavoritePasswordServiceTest {
    private val cryptoProvider = mockk<CryptoProvider>()
    private val eggRepository = mockk<EggRepository>()
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val favoritePasswordService = FavoritePasswordService(cryptoProvider, eggRepository, eventRegistry)

    @Test
    fun `should view favorites`() {
        val favorites = EggIdFavorites().apply { assign(Slot.S1, shellOf("favorite").fakeEnc()) }
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository)
        every { eggRepository.favorites() } returns favorites

        val actual = favoritePasswordService.viewFavorites()

        expectThat(actual[Slot.S1].get()) isEqualTo shellOf("favorite")
    }

    @Test
    fun `should assign favorite for existing egg`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("favorite"))
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))

        favoritePasswordService.putFavorite(Slot.S1, shellOf("favorite"))

        verify(exactly = 1) { eggRepository.putFavorite(Slot.S1, egg.viewEggId()) }
        verify(exactly = 1) { eggRepository.sync() }
    }

    @Test
    fun `should report not found when assigning favorite for missing egg`() {
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository)
        val eggNotFound = slot<EggNotFound>()

        favoritePasswordService.putFavorite(Slot.S1, shellOf("missing"))

        verify(exactly = 0) { eggRepository.putFavorite(any(), any()) }
        verify(exactly = 0) { eggRepository.sync() }
        verify(exactly = 1) { eventRegistry.register(capture(eggNotFound)) }
        expectThat(eggNotFound.captured.eggIdShell) isEqualTo shellOf("missing")
    }

    @Test
    fun `should discard favorite slot`() {
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository)

        favoritePasswordService.discardFavorite(Slot.S1)

        verify(exactly = 1) { eggRepository.discardFavorite(Slot.S1) }
        verify(exactly = 1) { eggRepository.sync() }
    }
}
