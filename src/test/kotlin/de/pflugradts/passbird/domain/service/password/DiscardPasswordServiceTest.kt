package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import de.pflugradts.passbird.domain.service.password.tree.fakeEggRepository
import io.mockk.Called
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
        verify(exactly = 1) { eggRepository.sync() }
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
        verify(exactly = 1) { eggRepository.sync() }
    }

    @Test
    fun `should discard existing protein`() {
        // given
        val eggId = shellOf("ProteinEgg")
        val type = shellOf("typeA")
        val structure = shellOf("structA")
        val egg = createEggForTesting(withEggIdShell = eggId)
        fakeCryptoProvider(instance = cryptoProvider)
        // add a protein manually using encrypted shells
        val encryptedType: EncryptedShell = cryptoProvider.encrypt(type)
        val encryptedStructure: EncryptedShell = cryptoProvider.encrypt(structure)
        egg.updateProtein(Slot.S1, encryptedType, encryptedStructure)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))
        expectThat(egg.proteins[Slot.S1.index()].isPresent).isTrue()

        // when
        passwordService.discardProtein(eggId, Slot.S1)

        // then
        expectThat(egg.proteins[Slot.S1.index()].isPresent).isNotEqualTo(true) // now empty
        verify(exactly = 1) { eventRegistry.processEvents() }
        verify(exactly = 1) { eggRepository.sync() }
    }

    @Test
    fun `should not register EggNotFound when discarding non-existent protein`() {
        // given
        val eggId = shellOf("ProteinEggNoSlot")
        val egg = createEggForTesting(withEggIdShell = eggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))
        expectThat(egg.proteins[Slot.S2.index()].isPresent).isNotEqualTo(true)

        // when
        passwordService.discardProtein(eggId, Slot.S2)

        // then
        verify(exactly = 0) { eventRegistry.register(any<EggNotFound>()) }
        verify(exactly = 1) { eventRegistry.processEvents() }
        verify(exactly = 1) { eggRepository.sync() }
    }

    @Test
    fun `should register EggNotFound when discarding protein of missing egg`() {
        // given
        val missingEggId = shellOf("MissingEgg")
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository) // empty repository
        val eggNotFoundSlot = slot<EggNotFound>()

        // when
        passwordService.discardProtein(missingEggId, Slot.S3)

        // then
        verify { eventRegistry.register(capture(eggNotFoundSlot)) }
        expectThat(eggNotFoundSlot.isCaptured).isTrue()
        expectThat(eggNotFoundSlot.captured.eggIdShell) isEqualTo missingEggId
        verify(exactly = 1) { eventRegistry.processEvents() }
        verify(exactly = 1) { eggRepository.sync() }
    }
}
