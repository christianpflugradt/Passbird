package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import de.pflugradts.passbird.domain.service.password.storage.fakeEggRepository
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.util.stream.Stream

class PutPasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val eggRepository = mockk<EggRepository>(relaxed = true)
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val passwordService = PutPasswordService(cryptoProvider, eggRepository, passbirdEventRegistry, nestService)

    @Nested
    inner class ChallengeEggIdTest {
        @Test
        fun `should succeed when challenging alphabetic eggId`() {
            // given
            val givenEggId = bytesOf("abcDEF")

            // when
            val actual = tryCatching { passwordService.challengeEggId(givenEggId) }

            // then
            expectThat(actual.success).isTrue()
        }

        @Test
        fun `should succeed when challenging eggId with digit at other than first position`() {
            // given
            val givenEggId = bytesOf("abc123")

            // when
            val actual = tryCatching { passwordService.challengeEggId(givenEggId) }

            // then
            expectThat(actual.success).isTrue()
        }

        @Test
        fun `should fail when challenging eggId with digit at first position`() {
            // given
            val givenEggId = bytesOf("123abc")

            // when
            val actual = tryCatching { passwordService.challengeEggId(givenEggId) }

            // then
            expectThat(actual.failure).isTrue()
            expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidEggIdException>()
        }

        @Test
        fun `should fail when challenging eggId with special characters`() {
            // given
            val givenEggId = bytesOf("abc!")

            // when
            val actual = tryCatching { passwordService.challengeEggId(givenEggId) }

            // then
            expectThat(actual.failure).isTrue()
            expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidEggIdException>()
        }
    }

    @Test
    fun `should insert new egg`() {
        // given
        val existingEggId = bytesOf("EggId")
        val newEggId = bytesOf("tryThis")
        val newPassword = bytesOf("Password")
        val matchingEgg = createEggForTesting(withEggIdBytes = existingEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEgg(newEggId, newPassword)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(newEggId) }
        verify(exactly = 1) { cryptoProvider.encrypt(newPassword) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { eggRepository.add(eq(createEgg(Slot.DEFAULT, newEggId, newPassword))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
    }

    @Test
    fun `should update existing egg`() {
        // given
        val existingEggId = bytesOf("EggId")
        val newPassword = bytesOf("Password")
        val matchingEgg = createEggForTesting(withEggIdBytes = existingEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEgg(existingEggId, newPassword)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(existingEggId) }
        verify(exactly = 1) { cryptoProvider.encrypt(newPassword) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(eggRepository.find(eggIdBytes = existingEggId).orElse(null).viewPassword()) isEqualTo newPassword
    }

    @Test
    fun `should reject invalid eggId`() {
        // given
        val invalidEggId = bytesOf("1EggId")

        // when
        val actual = tryCatching { passwordService.putEgg(invalidEggId, bytesOf("password")) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidEggIdException>()
        verify { cryptoProvider wasNot Called }
        verify { eggRepository wasNot Called }
    }

    @Test
    fun `should upsert multiple eggs`() {
        // given
        val newEggId = bytesOf("trythis")
        val newPassword = bytesOf("dont use this as a password")
        val existingEggId = bytesOf("EggId")
        val newPasswordForExistingEggId = bytesOf("Password")
        val matchingEgg = createEggForTesting(withEggIdBytes = existingEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEggs(
            Stream.of(BytePair(Pair(newEggId, newPassword)), BytePair(Pair(existingEggId, newPasswordForExistingEggId))),
        )

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(newEggId) }
        verify(exactly = 1) { cryptoProvider.encrypt(existingEggId) }
        verify(exactly = 1) { eggRepository.add(eq(createEgg(Slot.DEFAULT, newEggId, newPassword))) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(
            eggRepository.find(eggIdBytes = existingEggId).orElse(null).viewPassword(),
        ) isEqualTo newPasswordForExistingEggId
    }

    @Test
    fun `should accept empty stream`() {
        // given
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository)

        // when
        passwordService.putEggs(Stream.empty())

        // then
        verify(exactly = 0) { eggRepository.add(any()) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
    }
}
