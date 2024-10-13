package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import de.pflugradts.passbird.domain.service.password.tree.fakeEggRepository
import io.mockk.Called
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class ViewPasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val eggRepository = mockk<EggRepository>()
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val passwordService = ViewPasswordService(cryptoProvider, eggRepository, eventRegistry)

    @Test
    fun `should return true if egg exists`() {
        // given
        val givenEggId = shellOf("EggId")
        val matchingEgg = createEggForTesting(withEggIdShell = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        val actual = passwordService.eggExists(givenEggId, EggNotExistsAction.DO_NOTHING)

        // then
        verify { eventRegistry wasNot Called }
        expectThat(actual).isTrue()
    }

    @Test
    fun `should return false if egg does not exist`() {
        // given
        val givenEggId = shellOf("EggId")
        val otherEggId = shellOf("try this")
        val matchingEgg = createEggForTesting(withEggIdShell = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        val actual = passwordService.eggExists(otherEggId, EggNotExistsAction.DO_NOTHING)

        // then
        verify { eventRegistry wasNot Called }
        expectThat(actual).isFalse()
    }

    @Test
    fun `should find existing password`() {
        // given
        val givenEggId = shellOf("EggId")
        val expectedPassword = shellOf("Password")
        val matchingEgg = createEggForTesting(withEggIdShell = givenEggId, withPasswordShell = expectedPassword)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))
        val encryptedShellSlot = mutableListOf<EncryptedShell>()

        // when
        val actual = passwordService.viewPassword(givenEggId)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(givenEggId) }
        verify { cryptoProvider.decrypt(capture(encryptedShellSlot)) }
        expectThat(encryptedShellSlot.size) isEqualTo 2
        expectThat(encryptedShellSlot[0].fakeDec()) isEqualTo givenEggId
        expectThat(encryptedShellSlot[1].fakeDec()) isEqualTo expectedPassword
        verify { eventRegistry wasNot Called }
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get()) isEqualTo expectedPassword
    }

    @Test
    fun `should return empty optional if egg does not exist`() {
        // given
        val givenEggId = shellOf("EggId")
        val otherEggId = shellOf("tryThis")
        val matchingEgg = createEggForTesting(withEggIdShell = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))
        val eggNotFoundSlot = slot<EggNotFound>()

        // when
        val actual = passwordService.viewPassword(otherEggId)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(otherEggId) }
        verify { eventRegistry.register(capture(eggNotFoundSlot)) }
        expectThat(eggNotFoundSlot.isCaptured).isTrue()
        expectThat(eggNotFoundSlot.captured.eggIdShell.fakeDec()) isEqualTo otherEggId
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should find all eggIds in alphabetical order`() {
        // given
        val eggId1 = shellOf("abc")
        val eggId2 = shellOf("hij")
        val eggId3 = shellOf("xyz")
        val egg1 = createEggForTesting(withEggIdShell = eggId1)
        val egg2 = createEggForTesting(withEggIdShell = eggId2)
        val egg3 = createEggForTesting(withEggIdShell = eggId3)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(
            instance = eggRepository,
            withEggs = listOf(egg1, egg2, egg3),
        )

        // when
        val actual = passwordService.findAllEggIds()

        // then
        expectThat(actual.toList()).containsExactly(eggId1, eggId2, eggId3)
    }
}
