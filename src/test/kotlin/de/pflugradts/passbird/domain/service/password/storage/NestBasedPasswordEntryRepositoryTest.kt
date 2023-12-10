package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.createNestServiceSpyForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import strikt.java.isPresent
import java.util.function.Supplier
import java.util.stream.Stream

class NestBasedPasswordEntryRepositoryTest {

    private val givenPasswordEntry1 = createPasswordEntryForTesting(withKeyBytes = bytesOf("key1"))
    private val givenPasswordEntry2 = createPasswordEntryForTesting(withKeyBytes = bytesOf("key2"))
    private val givenPasswordEntries = listOf(givenPasswordEntry1, givenPasswordEntry2)

    private val passwordStoreAdapterPort = fakePasswordStoreAdapterPort(givenPasswordEntries)
    private val nestService = createNestServiceSpyForTesting()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val repository = NestBasedPasswordEntryRepository(passwordStoreAdapterPort, nestService, passbirdEventRegistry)

    @Test
    fun `should initialize`() {
        // given / when / then
        verify(exactly = 1) { passbirdEventRegistry.register(givenPasswordEntry1) }
        verify(exactly = 1) { passbirdEventRegistry.register(givenPasswordEntry2) }
    }

    @Test
    fun `should sync`() {
        // given
        val passwordEntriesSlot = slot<Supplier<Stream<PasswordEntry>>>()

        // when
        repository.sync()

        // then
        verify { passwordStoreAdapterPort.sync(capture(passwordEntriesSlot)) }
        expectThat(passwordEntriesSlot.captured.get().toList()).containsExactlyInAnyOrder(givenPasswordEntry1, givenPasswordEntry2)
    }

    @Test
    fun `should find password entry`() {
        // given / when
        val actual = repository.find(givenPasswordEntry1.viewKey())

        // then
        expectThat(actual).isPresent() isEqualTo givenPasswordEntry1
    }

    @Test
    fun `should return empty optional if requested password entry does not exist`() {
        // given
        val nonExistingPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf("unknown"))

        // when
        val actual = repository.find(nonExistingPasswordEntry.viewKey())

        // then
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should add password entry`() {
        // given
        val newPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf("new"))

        // when
        repository.add(newPasswordEntry)

        // then
        verify(exactly = 1) { passbirdEventRegistry.register(newPasswordEntry) }
        expectThat(repository.find(newPasswordEntry.viewKey())).isPresent() isEqualTo newPasswordEntry
    }

    @Test
    fun `should delete password entry`() {
        // given / when
        repository.delete(givenPasswordEntry1)

        // then
        expectThat(repository.find(givenPasswordEntry1.viewKey()).isEmpty).isTrue()
    }

    @Test
    fun `should find all`() {
        // given / when
        val actual = repository.findAll()

        // then
        expectThat(actual.toList()).containsExactlyInAnyOrder(givenPasswordEntry1, givenPasswordEntry2)
    }

    @Nested
    inner class NestTest {
        @Test
        fun `should find all in current nest`() {
            // given
            val activeNestSlot = Slot.N2
            val otherNestSlot = Slot.N3
            nestService.deploy(bytesOf("nest"), activeNestSlot)
            nestService.deploy(bytesOf("nest"), otherNestSlot)
            val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = bytesOf("first"), withNestSlot = activeNestSlot)
            val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = bytesOf("second"), withNestSlot = activeNestSlot)
            val passwordEntry3 = createPasswordEntryForTesting(withKeyBytes = bytesOf("third"), withNestSlot = otherNestSlot)
            repository.add(passwordEntry1)
            repository.add(passwordEntry2)
            repository.add(passwordEntry3)

            // when
            nestService.moveToNestAt(activeNestSlot)
            val actual = repository.findAll()

            // then
            expectThat(actual.toList()).containsExactlyInAnyOrder(passwordEntry1, passwordEntry2)
        }

        @Test
        fun `should store multiple password entries with identical keys in different nests`() {
            // given
            val keyBytes = bytesOf("key")
            val firstNestSlot = Slot.N1
            val secondNestSlot = Slot.N2
            nestService.deploy(bytesOf("nest"), firstNestSlot)
            nestService.deploy(bytesOf("nest"), secondNestSlot)
            val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = keyBytes, withNestSlot = firstNestSlot)
            val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = keyBytes, withNestSlot = secondNestSlot)
            repository.add(passwordEntry1)
            repository.add(passwordEntry2)

            // when
            nestService.moveToNestAt(firstNestSlot)
            val actualFirstEntry = repository.find(keyBytes)
            nestService.moveToNestAt(secondNestSlot)
            val actualSecondEntry = repository.find(keyBytes)

            // then
            expectThat(actualFirstEntry).isPresent() isEqualTo passwordEntry1 isNotEqualTo passwordEntry2
            expectThat(actualSecondEntry).isPresent() isEqualTo passwordEntry2 isNotEqualTo passwordEntry1
        }
    }
}
