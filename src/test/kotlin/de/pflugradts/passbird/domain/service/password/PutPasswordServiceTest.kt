package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.InvalidKeyException
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
    inner class ChallengeAliasTest {
        @Test
        fun `should succeed when challenging alphabetic alias`() {
            // given
            val givenAlias = bytesOf("abcDEF")

            // when
            val actual = tryCatching { passwordService.challengeAlias(givenAlias) }

            // then
            expectThat(actual.success).isTrue()
        }

        @Test
        fun `should succeed when challenging alias with digit at other than first position`() {
            // given
            val givenAlias = bytesOf("abc123")

            // when
            val actual = tryCatching { passwordService.challengeAlias(givenAlias) }

            // then
            expectThat(actual.success).isTrue()
        }

        @Test
        fun `should fail when challenging alias with digit at first position`() {
            // given
            val givenAlias = bytesOf("123abc")

            // when
            val actual = tryCatching { passwordService.challengeAlias(givenAlias) }

            // then
            expectThat(actual.failure).isTrue()
            expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidKeyException>()
        }

        @Test
        fun `should fail when challenging alias with special characters`() {
            // given
            val givenAlias = bytesOf("abc!")

            // when
            val actual = tryCatching { passwordService.challengeAlias(givenAlias) }

            // then
            expectThat(actual.failure).isTrue()
            expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidKeyException>()
        }
    }

    @Test
    fun `should insert new egg`() {
        // given
        val existingKey = bytesOf("Key")
        val newKey = bytesOf("tryThis")
        val newPassword = bytesOf("Password")
        val matchingEgg = createEggForTesting(withKeyBytes = existingKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEgg(newKey, newPassword)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(newKey) }
        verify(exactly = 1) { cryptoProvider.encrypt(newPassword) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { eggRepository.add(eq(createEgg(Slot.DEFAULT, newKey, newPassword))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
    }

    @Test
    fun `should update existing egg`() {
        // given
        val existingKey = bytesOf("Key")
        val newPassword = bytesOf("Password")
        val matchingEgg = createEggForTesting(withKeyBytes = existingKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEgg(existingKey, newPassword)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(existingKey) }
        verify(exactly = 1) { cryptoProvider.encrypt(newPassword) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(eggRepository.find(keyBytes = existingKey).orElse(null).viewPassword()) isEqualTo newPassword
    }

    @Test
    fun `should reject invalid key`() {
        // given
        val invalidKey = bytesOf("1Key")

        // when
        val actual = tryCatching { passwordService.putEgg(invalidKey, bytesOf("password")) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidKeyException>()
        verify { cryptoProvider wasNot Called }
        verify { eggRepository wasNot Called }
    }

    @Test
    fun `should upsert multiple eggs`() {
        // given
        val newKey = bytesOf("trythis")
        val newPassword = bytesOf("dont use this as a password")
        val existingKey = bytesOf("Key")
        val newPasswordForExistingKey = bytesOf("Password")
        val matchingEgg = createEggForTesting(withKeyBytes = existingKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEggs(
            Stream.of(BytePair(Pair(newKey, newPassword)), BytePair(Pair(existingKey, newPasswordForExistingKey))),
        )

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(newKey) }
        verify(exactly = 1) { cryptoProvider.encrypt(existingKey) }
        verify(exactly = 1) { eggRepository.add(eq(createEgg(Slot.DEFAULT, newKey, newPassword))) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(
            eggRepository.find(keyBytes = existingKey).orElse(null).viewPassword(),
        ) isEqualTo newPasswordForExistingKey
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
