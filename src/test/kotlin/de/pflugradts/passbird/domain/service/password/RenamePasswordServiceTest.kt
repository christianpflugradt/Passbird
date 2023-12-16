package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.EggIdAlreadyExistsException
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
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
        val oldEggId = shellOf("EggId123")
        val newEggId = shellOf("EggIdABC")
        val givenEgg = createEggForTesting(withEggIdShell = oldEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))

        // when
        passwordService.renameEgg(oldEggId, newEggId)

        // then
        expectThat(givenEgg.viewEggId()) isEqualTo newEggId isNotEqualTo oldEggId
    }

    @Test
    fun `should throw EggIdAlreadyExistsException if new eggId already exists`() {
        // given
        val oldEggId = shellOf("EggId123")
        val newEggId = shellOf("EggIdABC")
        val givenEgg = createEggForTesting(withEggIdShell = oldEggId)
        val existingEgg = createEggForTesting(withEggIdShell = newEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(
            instance = eggRepository,
            withEggs = listOf(givenEgg, existingEgg),
        )

        // when
        val actual = tryCatching { passwordService.renameEgg(oldEggId, newEggId) }

        // then
        expectThat(givenEgg.viewEggId()) isEqualTo oldEggId isNotEqualTo newEggId
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<EggIdAlreadyExistsException>()
    }

    @Test
    fun `should do nothing if egg does not exist`() {
        // given
        val oldEggId = shellOf("EggId123")
        val newEggId = shellOf("EggIdABC")
        val givenEgg = createEggForTesting(withEggIdShell = oldEggId)
        val existingEgg = createEggForTesting()
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(existingEgg))

        // when
        passwordService.renameEgg(oldEggId, newEggId)

        // then
        expectThat(givenEgg.viewEggId()) isEqualTo oldEggId isNotEqualTo newEggId
    }

    @Test
    fun `should reject invalid eggId`() {
        // given
        val oldEggId = shellOf("EggId123")
        val newEggId = shellOf("123")
        val givenEgg = createEggForTesting(withEggIdShell = oldEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))

        // when
        val actual = tryCatching { passwordService.renameEgg(oldEggId, newEggId) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull().isA<InvalidEggIdException>()
        verify { cryptoProvider wasNot Called }
        verify { eggRepository wasNot Called }
    }
}
