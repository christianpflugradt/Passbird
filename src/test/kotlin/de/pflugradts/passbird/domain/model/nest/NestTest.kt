package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class NestTest {
    @Test
    fun `should create nest`() {
        // given
        val name = "Nest"

        // when
        val actual = createNest(shellOf("Nest"), Slot.DEFAULT)

        // then
        expectThat(actual.viewNestId().asString()) isEqualTo name
    }

    @Test
    fun `should clone shell`() {
        // given
        val shell = shellOf("EggId")
        val nest = createNest(shell, Slot.DEFAULT)

        // when
        shell.scramble()
        val actual = nest.viewNestId()

        // then
        expectThat(actual) isNotEqualTo shell
    }

    @Test
    fun `should create default nest`() {
        // given / when
        val defaultNest = DEFAULT

        // then
        expectThat(defaultNest.viewNestId()) isEqualTo shellOf("Default")
        expectThat(defaultNest.slot) isEqualTo Slot.DEFAULT
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val givenSlot = S1
            val nest1 = createNest(shellOf("nest"), givenSlot)
            val nest2 = nest1

            // when
            val actual = nest1.equals(nest2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal if nest slot matches`() {
            // given
            val givenSlot = S1
            val nest1 = createNest(shellOf("nest"), givenSlot)
            val nest2 = createNest(shellOf("nest2"), givenSlot)

            // when
            val actual = nest1.equals(nest2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal if nest slot does not match`() {
            // given
            val givenSlot = S1
            val otherSlot = S2
            val nest1 = createNest(shellOf("nest"), givenSlot)
            val nest2 = createNest(shellOf("nest2"), otherSlot)

            // when
            val actual = nest1.equals(nest2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other classes`() {
            // given
            val givenSlot = S1
            val nest = createNest(shellOf("nest"), givenSlot)

            // when
            val actual = nest.equals(givenSlot)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val nest = createNest(shellOf("nest"), S1)

            // when
            val actual = nest.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
