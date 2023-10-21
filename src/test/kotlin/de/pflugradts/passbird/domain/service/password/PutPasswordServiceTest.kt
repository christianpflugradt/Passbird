package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.InvalidKeyException
import de.pflugradts.passbird.domain.model.password.PasswordEntry.Companion.createPasswordEntry
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.createNamespaceServiceForTesting
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import de.pflugradts.passbird.domain.service.password.storage.fakePasswordEntryRepository
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
    private val passwordEntryRepository = mockk<PasswordEntryRepository>(relaxed = true)
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val namespaceService = createNamespaceServiceForTesting()
    private val passwordService = PutPasswordService(cryptoProvider, passwordEntryRepository, passbirdEventRegistry, namespaceService)

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
    fun `should insert new password entry`() {
        // given
        val existingKey = bytesOf("Key")
        val newKey = bytesOf("tryThis")
        val newPassword = bytesOf("Password")
        val matchingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = existingKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(matchingPasswordEntry))

        // when
        passwordService.putPasswordEntry(newKey, newPassword)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(newKey) }
        verify(exactly = 1) { cryptoProvider.encrypt(newPassword) }
        verify(exactly = 1) { passwordEntryRepository.sync() }
        verify(exactly = 1) { passwordEntryRepository.add(eq(createPasswordEntry(NamespaceSlot.DEFAULT, newKey, newPassword))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
    }

    @Test
    fun `should update existing password entry`() {
        // given
        val existingKey = bytesOf("Key")
        val newPassword = bytesOf("Password")
        val matchingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = existingKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(matchingPasswordEntry))

        // when
        passwordService.putPasswordEntry(existingKey, newPassword)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(existingKey) }
        verify(exactly = 1) { cryptoProvider.encrypt(newPassword) }
        verify(exactly = 1) { passwordEntryRepository.sync() }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(passwordEntryRepository.find(keyBytes = existingKey).orElse(null).viewPassword()) isEqualTo newPassword
    }

    @Test
    fun `should reject invalid key`() {
        // given
        val invalidKey = bytesOf("1Key")

        // when
        val actual = tryCatching { passwordService.putPasswordEntry(invalidKey, bytesOf("password")) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidKeyException>()
        verify { cryptoProvider wasNot Called }
        verify { passwordEntryRepository wasNot Called }
    }

    @Test
    fun `should upsert multiple password entries`() {
        // given
        val newKey = bytesOf("trythis")
        val newPassword = bytesOf("dont use this as a password")
        val existingKey = bytesOf("Key")
        val newPasswordForExistingKey = bytesOf("Password")
        val matchingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = existingKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(matchingPasswordEntry))

        // when
        passwordService.putPasswordEntries(
            Stream.of(BytePair(Pair(newKey, newPassword)), BytePair(Pair(existingKey, newPasswordForExistingKey))),
        )

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(newKey) }
        verify(exactly = 1) { cryptoProvider.encrypt(existingKey) }
        verify(exactly = 1) { passwordEntryRepository.add(eq(createPasswordEntry(NamespaceSlot.DEFAULT, newKey, newPassword))) }
        verify(exactly = 1) { passwordEntryRepository.sync() }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(
            passwordEntryRepository.find(keyBytes = existingKey).orElse(null).viewPassword(),
        ) isEqualTo newPasswordForExistingKey
    }
}
