package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.InvalidKeyException
import de.pflugradts.passbird.domain.model.egg.KeyAlreadyExistsException
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import de.pflugradts.passbird.domain.service.password.storage.fakeEggRepository
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class RenamePasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val eggRepository = mockk<EggRepository>()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val passwordService = RenamePasswordService(cryptoProvider, eggRepository, passbirdEventRegistry)

    @Test
    fun `should rename egg`() {
        // given
        val oldKey = bytesOf("key123")
        val newKey = bytesOf("keyABC")
        val givenEgg = createEggForTesting(withKeyBytes = oldKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))

        // when
        passwordService.renameEgg(oldKey, newKey)

        // then
        expectThat(givenEgg.viewKey()) isEqualTo newKey isNotEqualTo oldKey
    }

    @Test
    fun `should throw KeyAlreadyExistsException if new alias already exists`() {
        // given
        val oldKey = bytesOf("key123")
        val newKey = bytesOf("keyABC")
        val givenEgg = createEggForTesting(withKeyBytes = oldKey)
        val existingEgg = createEggForTesting(withKeyBytes = newKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(
            instance = eggRepository,
            withEggs = listOf(givenEgg, existingEgg),
        )

        // when
        val actual = tryCatching { passwordService.renameEgg(oldKey, newKey) }

        // then
        expectThat(givenEgg.viewKey()) isEqualTo oldKey isNotEqualTo newKey
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<KeyAlreadyExistsException>()
    }

    @Test
    fun `should do nothing if egg does not exist`() {
        // given
        val oldKey = bytesOf("key123")
        val newKey = bytesOf("keyABC")
        val givenEgg = createEggForTesting(withKeyBytes = oldKey)
        val existingEgg = createEggForTesting()
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(existingEgg))

        // when
        passwordService.renameEgg(oldKey, newKey)

        // then
        expectThat(givenEgg.viewKey()) isEqualTo oldKey isNotEqualTo newKey
    }

    @Test
    fun `should reject invalid key`() {
        // given
        val oldKey = bytesOf("key123")
        val newKey = bytesOf("123")
        val givenEgg = createEggForTesting(withKeyBytes = oldKey)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))

        // when
        val actual = tryCatching { passwordService.renameEgg(oldKey, newKey) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidKeyException>()
        verify { cryptoProvider wasNot Called }
        verify { eggRepository wasNot Called }
    }
}
