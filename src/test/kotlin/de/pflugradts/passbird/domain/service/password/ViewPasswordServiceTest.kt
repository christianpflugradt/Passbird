package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import de.pflugradts.passbird.domain.service.password.storage.fakeEggRepository
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import strikt.java.isPresent

class ViewPasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val eggRepository = mockk<EggRepository>()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val passwordService = ViewPasswordService(cryptoProvider, eggRepository, passbirdEventRegistry)

    @Test
    fun `should return true if egg exists`() {
        // given
        val givenEggId = bytesOf("EggId")
        val matchingEgg = createEggForTesting(withEggIdBytes = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        val actual = passwordService.eggExists(givenEggId, EggNotExistsAction.DO_NOTHING)

        // then
        verify { passbirdEventRegistry wasNot Called }
        expectThat(actual).isTrue()
    }

    @Test
    fun `should return false if egg does not exist`() {
        // given
        val givenEggId = bytesOf("EggId")
        val otherEggId = bytesOf("try this")
        val matchingEgg = createEggForTesting(withEggIdBytes = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        val actual = passwordService.eggExists(otherEggId, EggNotExistsAction.DO_NOTHING)

        // then
        verify { passbirdEventRegistry wasNot Called }
        expectThat(actual).isFalse()
    }

    @Test
    fun `should find existing password`() {
        // given
        val givenEggId = bytesOf("EggId")
        val expectedPassword = bytesOf("Password")
        val matchingEgg = createEggForTesting(withEggIdBytes = givenEggId, withPasswordBytes = expectedPassword)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        val actual = passwordService.viewPassword(givenEggId)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(givenEggId) }
        verify(exactly = 1) { cryptoProvider.decrypt(expectedPassword) }
        verify { passbirdEventRegistry wasNot Called }
        expectThat(actual).isPresent() isEqualTo expectedPassword
    }

    @Test
    fun `should return empty optional if egg does not exist`() {
        // given
        val givenEggId = bytesOf("EggId")
        val otherEggId = bytesOf("tryThis")
        val matchingEgg = createEggForTesting(withEggIdBytes = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        val actual = passwordService.viewPassword(otherEggId)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(otherEggId) }
        verify(exactly = 1) { passbirdEventRegistry.register(eq(EggNotFound(otherEggId))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should find all eggIds in alphabetical order`() {
        // given
        val eggId1 = bytesOf("abc")
        val eggId2 = bytesOf("hij")
        val eggId3 = bytesOf("xyz")
        val egg1 = createEggForTesting(withEggIdBytes = eggId1)
        val egg2 = createEggForTesting(withEggIdBytes = eggId2)
        val egg3 = createEggForTesting(withEggIdBytes = eggId3)
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
