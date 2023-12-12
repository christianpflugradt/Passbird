
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.EggIdAlreadyExistsException
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.service.password.MovePasswordService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import de.pflugradts.passbird.domain.service.password.storage.fakeEggRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotNull

internal class MovePasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val eggRepository = mockk<EggRepository>()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val passwordService = MovePasswordService(cryptoProvider, eggRepository, passbirdEventRegistry)

    @Test
    fun `should move egg`() {
        // given
        val givenEggId = shellOf("eggId123")
        val givenNestSlot = NestSlot.N1
        val newNestSlot = NestSlot.N2
        val givenEgg = createEggForTesting(withEggIdShell = givenEggId, withNestSlot = givenNestSlot)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))

        // when
        passwordService.movePassword(givenEggId, newNestSlot)

        // then
        expectThat(givenEgg.associatedNest()) isEqualTo newNestSlot isNotEqualTo givenNestSlot
    }

    @Test
    fun `should not move egg if it does not exist`() {
        // given
        val givenEggId = shellOf("eggId123")
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository)

        // when
        passwordService.movePassword(givenEggId, NestSlot.N1)

        // then
        verify(exactly = 1) { passbirdEventRegistry.register(eq(EggNotFound(givenEggId))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
    }

    @Test
    fun `should not move egg if eggId already exists in target nest`() {
        // given
        val givenEggId = shellOf("eggId123")
        val givenNestSlot = NestSlot.N1
        val newNestSlot = NestSlot.N2
        val givenEgg = createEggForTesting(withEggIdShell = givenEggId, withNestSlot = givenNestSlot)
        val conflictingEgg = createEggForTesting(withEggIdShell = givenEggId, withNestSlot = newNestSlot)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(
            instance = eggRepository,
            withEggs = listOf(givenEgg, conflictingEgg),
        )

        // when
        val actual = tryCatching { passwordService.movePassword(givenEggId, newNestSlot) }

        // then
        expectThat(actual.exceptionOrNull()).isNotNull().isA<EggIdAlreadyExistsException>()
        expectThat(givenEgg.associatedNest()) isEqualTo givenNestSlot isNotEqualTo newNestSlot
    }
}
