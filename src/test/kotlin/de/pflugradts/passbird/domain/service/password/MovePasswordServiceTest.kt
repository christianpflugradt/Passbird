
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.KeyAlreadyExistsException
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
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
        val givenKey = bytesOf("key123")
        val givenNestSlot = Slot.N1
        val newNestSlot = Slot.N2
        val givenEgg = createEggForTesting(withKeyBytes = givenKey, withNestSlot = givenNestSlot)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(givenEgg))

        // when
        passwordService.movePassword(givenKey, newNestSlot)

        // then
        expectThat(givenEgg.associatedNest()) isEqualTo newNestSlot isNotEqualTo givenNestSlot
    }

    @Test
    fun `should not move egg if it does not exist`() {
        // given
        val givenKey = bytesOf("key123")
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository)

        // when
        passwordService.movePassword(givenKey, Slot.N1)

        // then
        verify(exactly = 1) { passbirdEventRegistry.register(eq(EggNotFound(givenKey))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
    }

    @Test
    fun `should not move egg if alias already exists in target nest`() {
        // given
        val givenKey = bytesOf("key123")
        val givenNestSlot = Slot.N1
        val newNestSlot = Slot.N2
        val givenEgg = createEggForTesting(withKeyBytes = givenKey, withNestSlot = givenNestSlot)
        val conflictingEgg = createEggForTesting(withKeyBytes = givenKey, withNestSlot = newNestSlot)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(
            instance = eggRepository,
            withEggs = listOf(givenEgg, conflictingEgg),
        )

        // when
        val actual = tryCatching { passwordService.movePassword(givenKey, newNestSlot) }

        // then
        expectThat(actual.exceptionOrNull()).isNotNull().isA<KeyAlreadyExistsException>()
        expectThat(givenEgg.associatedNest()) isEqualTo givenNestSlot isNotEqualTo newNestSlot
    }
}
