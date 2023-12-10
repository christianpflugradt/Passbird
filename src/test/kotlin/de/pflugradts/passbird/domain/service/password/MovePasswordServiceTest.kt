
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.password.KeyAlreadyExistsException
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.password.MovePasswordService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import de.pflugradts.passbird.domain.service.password.storage.fakePasswordEntryRepository
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
    private val passwordEntryRepository = mockk<PasswordEntryRepository>()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val passwordService = MovePasswordService(cryptoProvider, passwordEntryRepository, passbirdEventRegistry)

    @Test
    fun `should move password entry`() {
        // given
        val givenKey = bytesOf("key123")
        val givenNestSlot = Slot.N1
        val newNestSlot = Slot.N2
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey, withNestSlot = givenNestSlot)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(givenPasswordEntry))

        // when
        passwordService.movePassword(givenKey, newNestSlot)

        // then
        expectThat(givenPasswordEntry.associatedNest()) isEqualTo newNestSlot isNotEqualTo givenNestSlot
    }

    @Test
    fun `should not move password entry if it does not exist`() {
        // given
        val givenKey = bytesOf("key123")
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository)

        // when
        passwordService.movePassword(givenKey, Slot.N1)

        // then
        verify(exactly = 1) { passbirdEventRegistry.register(eq(PasswordEntryNotFound(givenKey))) }
        verify(exactly = 1) { passbirdEventRegistry.processEvents() }
    }

    @Test
    fun `should not move password entry if alias already exists in target nest`() {
        // given
        val givenKey = bytesOf("key123")
        val givenNestSlot = Slot.N1
        val newNestSlot = Slot.N2
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey, withNestSlot = givenNestSlot)
        val conflictingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey, withNestSlot = newNestSlot)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(
            instance = passwordEntryRepository,
            withPasswordEntries = listOf(givenPasswordEntry, conflictingPasswordEntry),
        )

        // when
        val actual = tryCatching { passwordService.movePassword(givenKey, newNestSlot) }

        // then
        expectThat(actual.exceptionOrNull()).isNotNull().isA<KeyAlreadyExistsException>()
        expectThat(givenPasswordEntry.associatedNest()) isEqualTo givenNestSlot isNotEqualTo newNestSlot
    }
}
