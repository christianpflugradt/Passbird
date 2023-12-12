package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
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
    fun `should view eggId`() {
        // given
        val givenBytes = bytesOf("myEggId")
        val egg = createEggForTesting(withEggIdBytes = givenBytes)

        // when
        val actual = egg.viewEggId()

        // then
        expectThat(actual) isEqualTo givenBytes
    }

    @Test
    fun `should rename eggId`() {
        // given
        val givenBytes = bytesOf("eggId123")
        val updatedBytes = bytesOf("eggIdABC")
        val egg = createEggForTesting(withEggIdBytes = givenBytes)

        // when
        egg.rename(updatedBytes)
        val actual = egg.viewEggId()

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
            val givenBytes = bytesOf("eggId")
            val givenNestSlot = Slot.N1
            val egg1 = createEggForTesting(withEggIdBytes = givenBytes, withNestSlot = givenNestSlot)
            val egg2 = egg1

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal if eggId and nest slot match`() {
            // given
            val givenBytes = bytesOf("eggId")
            val givenNestSlot = Slot.N1
            val egg1 = createEggForTesting(withEggIdBytes = givenBytes, withNestSlot = givenNestSlot)
            val egg2 = createEggForTesting(withEggIdBytes = givenBytes, withNestSlot = givenNestSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal if eggId does not match`() {
            // given
            val givenBytes = bytesOf("eggId")
            val otherBytes = bytesOf("eggId2")
            val givenNestSlot = Slot.N1
            val egg1 = createEggForTesting(withEggIdBytes = givenBytes, withNestSlot = givenNestSlot)
            val egg2 = createEggForTesting(withEggIdBytes = otherBytes, withNestSlot = givenNestSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal if nest slot does not match`() {
            // given
            val givenBytes = bytesOf("eggId")
            val givenNestSlot = Slot.N1
            val otherNestSlot = Slot.N2
            val egg1 = createEggForTesting(withEggIdBytes = givenBytes, withNestSlot = givenNestSlot)
            val egg2 = createEggForTesting(withEggIdBytes = givenBytes, withNestSlot = otherNestSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other classes`() {
            // given
            val givenBytes = bytesOf("eggId")
            val egg = createEggForTesting(withEggIdBytes = givenBytes)

            // when
            val actual = egg.equals(createEggId(givenBytes))

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val egg = createEggForTesting(withEggIdBytes = bytesOf("eggId"))

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
            val egg = createEgg(Slot.DEFAULT, bytesOf("eggId"), bytesOf("password"))

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
