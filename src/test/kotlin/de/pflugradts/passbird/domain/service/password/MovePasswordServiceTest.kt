
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.KeyAlreadyExistsException
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.password.MovePasswordService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import de.pflugradts.passbird.domain.service.password.storage.fakePasswordEntryRepository
import io.mockk.mockk
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
        val givenNamespace = NamespaceSlot.N1
        val newNamespace = NamespaceSlot.N2
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey, withNamespace = givenNamespace)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(instance = passwordEntryRepository, withPasswordEntries = listOf(givenPasswordEntry))

        // when
        passwordService.movePassword(givenKey, newNamespace)

        // then
        expectThat(givenPasswordEntry.associatedNamespace()) isEqualTo newNamespace isNotEqualTo givenNamespace
    }

    @Test
    fun `should not move password entry if alias already exists in target namespace`() {
        // given
        val givenKey = bytesOf("key123")
        val givenNamespace = NamespaceSlot.N1
        val newNamespace = NamespaceSlot.N2
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey, withNamespace = givenNamespace)
        val conflictingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = givenKey, withNamespace = newNamespace)
        fakeCryptoProvider(instance = cryptoProvider)
        fakePasswordEntryRepository(
            instance = passwordEntryRepository,
            withPasswordEntries = listOf(givenPasswordEntry, conflictingPasswordEntry),
        )

        // when
        val actual = tryCatching { passwordService.movePassword(givenKey, newNamespace) }

        // then
        expectThat(actual.exceptionOrNull()).isNotNull().isA<KeyAlreadyExistsException>()
        expectThat(givenPasswordEntry.associatedNamespace()) isEqualTo givenNamespace isNotEqualTo newNamespace
    }
}
