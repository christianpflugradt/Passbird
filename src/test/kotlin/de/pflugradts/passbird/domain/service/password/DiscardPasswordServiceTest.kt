package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import de.pflugradts.passbird.domain.service.password.tree.fakeEggRepository
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

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
        expectThat(givenEgg.viewPassword().fakeDec()) isEqualTo givenPassword
        passwordService.discardEgg(givenEggId)

        // then
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(givenEgg.viewPassword()) isNotEqualTo givenPassword.fakeEnc()
    }

    @Test
    fun `should not discard anything if there is no match`() {
        // given
        val givenEggId = shellOf("EggId")
        val otherEggId = shellOf("try this")
        val givenEgg = createEggForTesting(withEggIdShell = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))
        val eggNotFoundSlot = slot<EggNotFound>()

        // when
        passwordService.discardEgg(otherEggId)

        // then
        verify { eventRegistry.register(capture(eggNotFoundSlot)) }
        expectThat(eggNotFoundSlot.isCaptured).isTrue()
        expectThat(eggNotFoundSlot.captured.eggIdShell) isEqualTo otherEggId
        verify(exactly = 1) { eventRegistry.processEvents() }
    }
}
