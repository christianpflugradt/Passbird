package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import de.pflugradts.passbird.domain.service.password.storage.fakePasswordEntryRepository
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
    private val passwordEntryRepository = mockk<PasswordEntryRepository>()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val passwordService = ViewPasswordService(cryptoProvider, passwordEntryRepository, passbirdEventRegistry)

    @Test
    fun `should return true if entry exists`() {
        // given
        val givenKey = bytesOf("Key")
        val matchingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(matchingPasswordEntry))

        // when
        val actual = passwordService.entryExists(givenKey, EntryNotExistsAction.DO_NOTHING)

        // then
        verify { passbirdEventRegistry wasNot Called }
        expectThat(actual).isTrue()
    }

    @Test
    fun `should return false if entry does not exist`() {
        // given
        val givenKey = bytesOf("Key")
        val otherKey = bytesOf("try this")
        val matchingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(matchingPasswordEntry))

        // when
        val actual = passwordService.entryExists(otherKey, EntryNotExistsAction.DO_NOTHING)

        // then
        verify { passbirdEventRegistry wasNot Called }
        expectThat(actual).isFalse()
    }

    @Test
    fun `should find existing password`() {
        // given
        val givenKey = bytesOf("Key")
        val expectedPassword = bytesOf("Password")
        val matchingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey, withPasswordBytes = expectedPassword)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(matchingPasswordEntry))

        // when
        val actual = passwordService.viewPassword(givenKey)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(givenKey) }
        verify(exactly = 1) { cryptoProvider.decrypt(expectedPassword) }
        verify { passbirdEventRegistry wasNot Called }
        expectThat(actual).isPresent() isEqualTo expectedPassword
    }

    @Test
    fun `should return empty optional if password entry does not exist`() {
        // given
        val givenKey = bytesOf("Key")
        val otherKey = bytesOf("tryThis")
        val matchingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(matchingPasswordEntry))

        // when
        val actual = passwordService.viewPassword(otherKey)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(otherKey) }
        verify(exactly = 1) { passbirdEventRegistry.register(eq(PasswordEntryNotFound(otherKey))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should find all keys in alphabetical order`() {
        // given
        val key1 = bytesOf("abc")
        val key2 = bytesOf("hij")
        val key3 = bytesOf("xyz")
        val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = key1)
        val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = key2)
        val passwordEntry3 = createPasswordEntryForTesting(withKeyBytes = key3)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(
            instance = passwordEntryRepository,
            withPasswordEntries = listOf(passwordEntry1, passwordEntry2, passwordEntry3),
        )

        // when
        val actual = passwordService.findAllKeys()

        // then
        expectThat(actual.toList()).containsExactly(key1, key2, key3)
    }
}
