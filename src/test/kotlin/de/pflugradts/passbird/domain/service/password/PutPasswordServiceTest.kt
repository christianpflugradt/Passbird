package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
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
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val passwordService = PutPasswordService(cryptoProvider, eggRepository, eventRegistry, nestService)

    @Nested
    inner class ChallengeEggIdTest {
        @Test
        fun `should succeed when challenging alphabetic eggId`() {
            // given
            val givenEggId = shellOf("abcDEF")

            // when
            val actual = tryCatching { passwordService.challengeEggId(givenEggId) }

            // then
            expectThat(actual.success).isTrue()
        }

        @Test
        fun `should succeed when challenging eggId with digit at other than first position`() {
            // given
            val givenEggId = shellOf("abc123")

            // when
            val actual = tryCatching { passwordService.challengeEggId(givenEggId) }

            // then
            expectThat(actual.success).isTrue()
        }

        @Test
        fun `should fail when challenging eggId with digit at first position`() {
            // given
            val givenEggId = shellOf("123abc")

            // when
            val actual = tryCatching { passwordService.challengeEggId(givenEggId) }

            // then
            expectThat(actual.failure).isTrue()
            expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidEggIdException>()
        }

        @Test
        fun `should fail when challenging eggId with special characters`() {
            // given
            val givenEggId = shellOf("abc!")

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
        val existingEggId = shellOf("EggId")
        val newEggId = shellOf("tryThis")
        val newPassword = shellOf("Password")
        val matchingEgg = createEggForTesting(withEggIdShell = existingEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEgg(newEggId, newPassword)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(newEggId) }
        verify(exactly = 1) { cryptoProvider.encrypt(newPassword) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { eggRepository.add(eq(createEgg(NestSlot.DEFAULT, newEggId, newPassword))) }
        verify(exactly = 1) { eventRegistry.processEvents() }
    }

    @Test
    fun `should update existing egg`() {
        // given
        val existingEggId = shellOf("EggId")
        val newPassword = shellOf("Password")
        val matchingEgg = createEggForTesting(withEggIdShell = existingEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEgg(existingEggId, newPassword)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(existingEggId) }
        verify(exactly = 1) { cryptoProvider.encrypt(newPassword) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(eggRepository.find(eggIdShell = existingEggId).orElse(null).viewPassword()) isEqualTo newPassword
    }

    @Test
    fun `should reject invalid eggId`() {
        // given
        val invalidEggId = shellOf("1EggId")

        // when
        val actual = tryCatching { passwordService.putEgg(invalidEggId, shellOf("Password")) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidEggIdException>()
        verify { cryptoProvider wasNot Called }
        verify { eggRepository wasNot Called }
    }

    @Test
    fun `should upsert multiple eggs`() {
        // given
        val newEggId = shellOf("trythis")
        val newPassword = shellOf("dont use this as a password")
        val existingEggId = shellOf("EggId")
        val newPasswordForExistingEggId = shellOf("Password")
        val matchingEgg = createEggForTesting(withEggIdShell = existingEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        passwordService.putEggs(
            Stream.of(ShellPair(newEggId, newPassword), ShellPair(existingEggId, newPasswordForExistingEggId)),
        )

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(newEggId) }
        verify(exactly = 1) { cryptoProvider.encrypt(existingEggId) }
        verify(exactly = 1) { eggRepository.add(eq(createEgg(NestSlot.DEFAULT, newEggId, newPassword))) }
        verify(exactly = 1) { eggRepository.sync() }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(
            eggRepository.find(eggIdShell = existingEggId).orElse(null).viewPassword(),
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
        verify(exactly = 1) { eventRegistry.processEvents() }
    }
}
