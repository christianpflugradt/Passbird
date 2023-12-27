package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import de.pflugradts.passbird.domain.service.password.storage.fakeEggRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class DiscardPasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val eggRepository = mockk<EggRepository>()
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val passwordService = DiscardPasswordService(cryptoProvider, eggRepository, eventRegistry)

    @Test
    fun `should discard egg`() {
        // given
        val givenEggId = shellOf("EggId")
        val givenPassword = shellOf("Password")
        val givenEgg = createEggForTesting(withEggIdShell = givenEggId, withPasswordShell = givenPassword)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))

        // when
        expectThat(givenEgg.viewPassword()) isEqualTo givenPassword
        passwordService.discardEgg(givenEggId)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(givenEggId) }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(givenEgg.viewPassword()) isNotEqualTo givenPassword
    }

    @Test
    fun `should not discard anything if there is no match`() {
        // given
        val givenEggId = shellOf("EggId")
        val otherEggId = shellOf("try this")
        val givenEgg = createEggForTesting(withEggIdShell = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))

        // when
        passwordService.discardEgg(otherEggId)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(otherEggId) }
        verify(exactly = 1) { eventRegistry.register(eq(EggNotFound(otherEggId))) }
        verify(exactly = 1) { eventRegistry.processEvents() }
    }
}
