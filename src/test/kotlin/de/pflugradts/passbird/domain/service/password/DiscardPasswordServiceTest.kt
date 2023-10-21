package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import de.pflugradts.passbird.domain.service.password.storage.fakePasswordEntryRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class DiscardPasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val passwordEntryRepository = mockk<PasswordEntryRepository>()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val passwordService = DiscardPasswordService(cryptoProvider, passwordEntryRepository, passbirdEventRegistry)

    @Test
    fun `should discard password entry`() {
        // given
        val givenKey = bytesOf("Key")
        val givenPassword = bytesOf("Password")
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey, withPasswordBytes = givenPassword)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(givenPasswordEntry))

        // when
        expectThat(givenPasswordEntry.viewPassword()) isEqualTo givenPassword
        passwordService.discardPasswordEntry(givenKey)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(givenKey) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
        expectThat(givenPasswordEntry.viewPassword()) isNotEqualTo givenPassword
    }

    @Test
    fun `should not discard anything if there is no match`() {
        // given
        val givenKey = bytesOf("Key")
        val otherKey = bytesOf("try this")
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(givenPasswordEntry))

        // when
        passwordService.discardPasswordEntry(otherKey)

        // then
        verify(exactly = 1) { cryptoProvider.encrypt(otherKey) }
        verify(exactly = 1) { passbirdEventRegistry.register(eq(PasswordEntryNotFound(otherKey))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
    }
}
