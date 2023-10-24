package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.event.PasswordEntryCreated
import de.pflugradts.passbird.domain.model.event.PasswordEntryDiscarded
import de.pflugradts.passbird.domain.model.event.PasswordEntryUpdated
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.Key.Companion.createKey
import de.pflugradts.passbird.domain.model.password.PasswordEntry.Companion.createPasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class PasswordEntryTest {
    @Test
    fun `should view key`() {
        // given
        val givenBytes = bytesOf("myKey")
        val passwordEntry = createPasswordEntryForTesting(withKeyBytes = givenBytes)

        // when
        val actual = passwordEntry.viewKey()

        // then
        expectThat(actual) isEqualTo givenBytes
    }

    @Test
    fun `should rename key`() {
        // given
        val givenBytes = bytesOf("key123")
        val updatedBytes = bytesOf("keyABC")
        val passwordEntry = createPasswordEntryForTesting(withKeyBytes = givenBytes)

        // when
        passwordEntry.rename(updatedBytes)
        val actual = passwordEntry.viewKey()

        // then
        expectThat(actual) isEqualTo updatedBytes isNotEqualTo givenBytes
    }

    @Test
    fun `should view password`() {
        // given
        val givenBytes = bytesOf("myPassword")
        val passwordEntry = createPasswordEntryForTesting(withPasswordBytes = givenBytes)

        // when
        val actual = passwordEntry.viewPassword()

        // then
        expectThat(actual) isEqualTo givenBytes
    }

    @Test
    fun `should update password`() {
        // given
        val givenBytes = bytesOf("myPassword")
        val updatedBytes = bytesOf("newPassword")
        val passwordEntry = createPasswordEntryForTesting(withPasswordBytes = givenBytes)

        // when
        passwordEntry.updatePassword(updatedBytes)
        val actual = passwordEntry.viewPassword()

        // then
        expectThat(actual) isEqualTo updatedBytes isNotEqualTo givenBytes
    }

    @Test
    fun `should discard`() {
        // given
        val givenBytes = mockk<Bytes>(relaxed = true)
        every { givenBytes.copy() } returns givenBytes
        val passwordEntry = createPasswordEntryForTesting(withPasswordBytes = givenBytes)

        // when
        passwordEntry.discard()

        // then
        verify { givenBytes.scramble() }
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val givenBytes = bytesOf("key")
            val givenNamespace = NamespaceSlot.N1
            val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = givenBytes, withNamespace = givenNamespace)
            val passwordEntry2 = passwordEntry1

            // when
            val actual = passwordEntry1.equals(passwordEntry2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal if key and namespace match`() {
            // given
            val givenBytes = bytesOf("key")
            val givenNamespace = NamespaceSlot.N1
            val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = givenBytes, withNamespace = givenNamespace)
            val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = givenBytes, withNamespace = givenNamespace)

            // when
            val actual = passwordEntry1.equals(passwordEntry2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal if key does not match`() {
            // given
            val givenBytes = bytesOf("key")
            val otherBytes = bytesOf("key2")
            val givenNamespace = NamespaceSlot.N1
            val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = givenBytes, withNamespace = givenNamespace)
            val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = otherBytes, withNamespace = givenNamespace)

            // when
            val actual = passwordEntry1.equals(passwordEntry2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal if namespace does not match`() {
            // given
            val givenBytes = bytesOf("key")
            val givenNamespace = NamespaceSlot.N1
            val otherNamespace = NamespaceSlot.N2
            val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = givenBytes, withNamespace = givenNamespace)
            val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = givenBytes, withNamespace = otherNamespace)

            // when
            val actual = passwordEntry1.equals(passwordEntry2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other classes`() {
            // given
            val givenBytes = bytesOf("key")
            val passwordEntry = createPasswordEntryForTesting(withKeyBytes = givenBytes)

            // when
            val actual = passwordEntry.equals(createKey(givenBytes))

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val passwordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf("key"))

            // when
            val actual = passwordEntry.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }

    @Nested
    inner class DomainEventsTest {
        @Test
        fun `should have created event when password entry is created`() {
            // given / when
            val passwordEntry = createPasswordEntry(NamespaceSlot.DEFAULT, bytesOf("key"), bytesOf("password"))

            // then
            expectThat(passwordEntry.getDomainEvents()) hasSize 1
            val actual = passwordEntry.getDomainEvents()[0]
            expectThat(actual).isA<PasswordEntryCreated>()
            expectThat((actual as PasswordEntryCreated).passwordEntry) isEqualTo passwordEntry
        }

        @Test
        fun `should have updated event when password is updated`() {
            // given
            val passwordEntry = createPasswordEntryForTesting()

            // when
            passwordEntry.clearDomainEvents()
            passwordEntry.updatePassword(bytesOf("new password"))

            // then
            expectThat(passwordEntry.getDomainEvents()) hasSize 1
            val actual = passwordEntry.getDomainEvents()[0]
            expectThat(actual).isA<PasswordEntryUpdated>()
            expectThat((actual as PasswordEntryUpdated).passwordEntry) isEqualTo passwordEntry
        }

        @Test
        fun `should have discarded event when password entry is discarded`() {
            // given
            val passwordEntry = createPasswordEntryForTesting()

            // when
            passwordEntry.clearDomainEvents()
            passwordEntry.discard()

            // then
            expectThat(passwordEntry.getDomainEvents()) hasSize 1
            val actual = passwordEntry.getDomainEvents()[0]
            expectThat(actual).isA<PasswordEntryDiscarded>()
            expectThat((actual as PasswordEntryDiscarded).passwordEntry) isEqualTo passwordEntry
        }
    }
}
