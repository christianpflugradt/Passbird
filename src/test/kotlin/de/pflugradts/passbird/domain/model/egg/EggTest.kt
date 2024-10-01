package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.S1
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
        val givenEggIdShell = shellOf("myEggId")
        val egg = createEggForTesting(withEggIdShell = givenEggIdShell)

        // when
        val actual = egg.viewEggId()

        // then
        expectThat(actual.fakeDec()) isEqualTo givenEggIdShell
    }

    @Test
    fun `should rename eggId`() {
        // given
        val givenEggIdShell = shellOf("EggId123")
        val updatedEggIdShell = shellOf("EggIdABC")
        val egg = createEggForTesting(withEggIdShell = givenEggIdShell)

        // when
        egg.rename(updatedEggIdShell.fakeEnc())
        val actual = egg.viewEggId()

        // then
        expectThat(actual.fakeDec()) isEqualTo updatedEggIdShell isNotEqualTo givenEggIdShell
    }

    @Test
    fun `should view password`() {
        // given
        val givenEggIdShell = shellOf("myPassword")
        val egg = createEggForTesting(withPasswordShell = givenEggIdShell)

        // when
        val actual = egg.viewPassword()

        // then
        expectThat(actual.fakeDec()) isEqualTo givenEggIdShell
    }

    @Test
    fun `should update password`() {
        // given
        val givenEggIdShell = shellOf("myPassword")
        val updatedEggIdShell = shellOf("newPassword")
        val egg = createEggForTesting(withPasswordShell = givenEggIdShell)

        // when
        egg.updatePassword(updatedEggIdShell.fakeEnc())
        val actual = egg.viewPassword()

        // then
        expectThat(actual.fakeDec()) isEqualTo updatedEggIdShell isNotEqualTo givenEggIdShell
    }

    @Test
    fun `should discard`() {
        // given
        val givenShell = mockk<Shell>(relaxed = true)
        every { givenShell.copy() } returns givenShell
        val egg = createEggForTesting(withPasswordShell = givenShell)

        // when
        egg.discard()

        // then
        verify { givenShell.scramble() }
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val givenEggIdShell = shellOf("EggId")
            val givenSlot = Slot.S1
            val egg1 = createEggForTesting(withEggIdShell = givenEggIdShell, withSlot = givenSlot)
            val egg2 = egg1

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal if eggId and nest slot match`() {
            // given
            val givenEggIdShell = shellOf("EggId")
            val givenSlot = Slot.S1
            val egg1 = createEggForTesting(withEggIdShell = givenEggIdShell, withSlot = givenSlot)
            val egg2 = createEggForTesting(withEggIdShell = givenEggIdShell, withSlot = givenSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal if eggId does not match`() {
            // given
            val givenEggIdShell = shellOf("EggId")
            val otherEggIdShell = shellOf("EggId2")
            val givenSlot = Slot.S1
            val egg1 = createEggForTesting(withEggIdShell = givenEggIdShell, withSlot = givenSlot)
            val egg2 = createEggForTesting(withEggIdShell = otherEggIdShell, withSlot = givenSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal if nest slot does not match`() {
            // given
            val givenEggIdShell = shellOf("EggId")
            val givenSlot = Slot.S1
            val otherSlot = Slot.S2
            val egg1 = createEggForTesting(withEggIdShell = givenEggIdShell, withSlot = givenSlot)
            val egg2 = createEggForTesting(withEggIdShell = givenEggIdShell, withSlot = otherSlot)

            // when
            val actual = egg1.equals(egg2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other classes`() {
            // given
            val givenEggIdShell = shellOf("EggId")
            val egg = createEggForTesting(withEggIdShell = givenEggIdShell)

            // when
            val actual = egg.equals(createEggId(givenEggIdShell.fakeEnc()))

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))

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
            val egg = createEgg(Slot.DEFAULT, shellOf("EggId").fakeEnc(), shellOf("Password").fakeEnc())

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
            egg.updatePassword(shellOf("new password").fakeEnc())

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

        @Test
        fun `should have renamed event when egg is renamed`() {
            // given
            val egg = createEggForTesting()

            // when
            egg.clearDomainEvents()
            egg.rename(shellOf("newEggId").fakeEnc())

            // then
            expectThat(egg.getDomainEvents()) hasSize 1
            val actual = egg.getDomainEvents()[0]
            expectThat(actual).isA<EggRenamed>()
            expectThat((actual as EggRenamed).egg) isEqualTo egg
        }

        @Test
        fun `should have moved event when egg is moved`() {
            // given
            val egg = createEggForTesting()

            // when
            egg.clearDomainEvents()
            egg.moveToNestAt(S1)

            // then
            expectThat(egg.getDomainEvents()) hasSize 1
            val actual = egg.getDomainEvents()[0]
            expectThat(actual).isA<EggMoved>()
            expectThat((actual as EggMoved).egg) isEqualTo egg
        }
    }
}
