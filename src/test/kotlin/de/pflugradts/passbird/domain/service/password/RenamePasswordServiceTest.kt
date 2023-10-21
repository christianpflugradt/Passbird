package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import de.pflugradts.passbird.domain.service.password.storage.fakePasswordEntryRepository
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class RenamePasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val passwordEntryRepository = mockk<PasswordEntryRepository>()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val passwordService = RenamePasswordService(cryptoProvider, passwordEntryRepository, passbirdEventRegistry)

    @Test
    fun `should rename password entry`() {
        // given
        val oldKey = bytesOf("key123")
        val newKey = bytesOf("keyABC")
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = oldKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(givenPasswordEntry))

        // when
        passwordService.renamePasswordEntry(oldKey, newKey)

        // then
        expectThat(givenPasswordEntry.viewKey()) isEqualTo newKey isNotEqualTo oldKey
    }

    @Test
    fun `should do nothing if entry does not exist`() {
        // given
        val oldKey = bytesOf("key123")
        val newKey = bytesOf("keyABC")
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = oldKey)
        val existingPasswordEntry = createPasswordEntryForTesting()
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(existingPasswordEntry))

        // when
        passwordService.renamePasswordEntry(oldKey, newKey)

        // then
        expectThat(givenPasswordEntry.viewKey()) isEqualTo oldKey isNotEqualTo newKey
    }
}