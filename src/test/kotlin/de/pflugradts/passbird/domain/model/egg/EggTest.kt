package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.Key.Companion.createKey
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.nest.Slot
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

class EggTest {
    @Test
    fun `should view key`() {
        // given
        val givenBytes = bytesOf("myKey")
        val egg = createEggForTesting(withKeyBytes = givenBytes)

        // when
        val actual = egg.viewKey()

        // then
        expectThat(actual) isEqualTo givenBytes
    }

    @Test
    fun `should rename key`() {
        // given
        val givenBytes = bytesOf("key123")
        val updatedBytes = bytesOf("keyABC")
        val egg = createEggForTesting(withKeyBytes = givenBytes)

        // when
        egg.rename(updatedBytes)
        val actual = egg.viewKey()

        // then
        expectThat(actual) isEqualTo updatedBytes isNotEqualTo givenBytes
    }

    @Test
    fun `should view password`() {
        // given
        val givenBytes = bytesOf("myPassword")
        val egg = createEggForTesting(withPasswordBytes = givenBytes)

        // when
        val actual = egg.viewPassword()

        // then
        expectThat(actual) isEqualTo givenBytes
    }

    @Test
    fun `should update password`() {
        // given
        val givenBytes = bytesOf("myPassword")
        val updatedBytes = bytesOf("newPassword")
        val egg = createEggForTesting(withPasswordBytes = givenBytes)

        // when
        egg.updatePassword(updatedBytes)
        val actual = egg.viewPassword()

        // then
        expectThat(actual) isEqualTo updatedBytes isNotEqualTo givenBytes
    }

    @Test
    fun `should discard`() {
        // given
        val givenBytes = mockk<Bytes>(relaxed = true)
        every { givenBytes.copy() } returns givenBytes
        val egg = createEggForTesting(withPasswordBytes = givenBytes)

        // when
        egg.discard()

        // then
        verify { givenBytes.scramble() }
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val givenBytes = bytesOf("key")
            val givenNestSlot = Slot.N1
            val egg1 = createEggForTesting(withKeyBytes = givenBytes, withNestSlot = givenNestSlot)
            val egg2 = egg1

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal if key and nest slot match`() {
            // given
            val givenBytes = bytesOf("key")
            val givenNestSlot = Slot.N1
            val egg1 = createEggForTesting(withKeyBytes = givenBytes, withNestSlot = givenNestSlot)
            val egg2 = createEggForTesting(withKeyBytes = givenBytes, withNestSlot = givenNestSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal if key does not match`() {
            // given
            val givenBytes = bytesOf("key")
            val otherBytes = bytesOf("key2")
            val givenNestSlot = Slot.N1
            val egg1 = createEggForTesting(withKeyBytes = givenBytes, withNestSlot = givenNestSlot)
            val egg2 = createEggForTesting(withKeyBytes = otherBytes, withNestSlot = givenNestSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal if nest slot does not match`() {
            // given
            val givenBytes = bytesOf("key")
            val givenNestSlot = Slot.N1
            val otherNestSlot = Slot.N2
            val egg1 = createEggForTesting(withKeyBytes = givenBytes, withNestSlot = givenNestSlot)
            val egg2 = createEggForTesting(withKeyBytes = givenBytes, withNestSlot = otherNestSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other classes`() {
            // given
            val givenBytes = bytesOf("key")
            val egg = createEggForTesting(withKeyBytes = givenBytes)

            // when
            val actual = egg.equals(createKey(givenBytes))

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val egg = createEggForTesting(withKeyBytes = bytesOf("key"))

            // when
            val actual = egg.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }

    @Nested
    inner class DomainEventsTest {
        @Test
        fun `should have created event when egg is created`() {
            // given / when
            val egg = createEgg(Slot.DEFAULT, bytesOf("key"), bytesOf("password"))

            // then
            expectThat(egg.getDomainEvents()) hasSize 1
            val actual = egg.getDomainEvents()[0]
            expectThat(actual).isA<EggCreated>()
            expectThat((actual as EggCreated).egg) isEqualTo egg
        }

        @Test
        fun `should have updated event when password is updated`() {
            // given
            val egg = createEggForTesting()

            // when
            egg.clearDomainEvents()
            egg.updatePassword(bytesOf("new password"))

            // then
            expectThat(egg.getDomainEvents()) hasSize 1
            val actual = egg.getDomainEvents()[0]
            expectThat(actual).isA<EggUpdated>()
            expectThat((actual as EggUpdated).egg) isEqualTo egg
        }

        @Test
        fun `should have discarded event when egg is discarded`() {
            // given
            val egg = createEggForTesting()

            // when
            egg.clearDomainEvents()
            egg.discard()

            // then
            expectThat(egg.getDomainEvents()) hasSize 1
            val actual = egg.getDomainEvents()[0]
            expectThat(actual).isA<EggDiscarded>()
            expectThat((actual as EggDiscarded).egg) isEqualTo egg
        }
    }
}
