package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class EggIdTest {

    @Test
    fun `should create eggId`() {
        // given
        val eggIdShell = shellOf("eggId")

        // when
        val eggId = createEggId(eggIdShell)

        // then
        expectThat(eggId.view()) isEqualTo eggIdShell
    }

    @Test
    fun `should rename eggId`() {
        // given
        val originalEggIdShell = shellOf("eggId123")
        val eggId = createEggId(originalEggIdShell)
        val updatedEggIdShell = shellOf("eggIdABC")

        // when
        eggId.rename(updatedEggIdShell)
        val actual = eggId.view()

        // then
        expectThat(actual) isEqualTo updatedEggIdShell isNotEqualTo originalEggIdShell
    }

    @Test
    fun `should clone eggIdShell`() {
        // given
        val eggIdShell = shellOf("eggId")
        val eggId = createEggId(eggIdShell)

        // when
        eggIdShell.scramble()
        val actual = eggId.view()

        // then
        expectThat(actual) isNotEqualTo eggIdShell
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val eggId1 = createEggId(shellOf("abc"))
            val eggId2 = eggId1

            // when
            val actual = eggId1.equals(eggId2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to eggId with equal eggIdShell`() {
            // given
            val eggIdShell = shellOf("abc")
            val sameEggIdShell = shellOf("abc")
            val eggId1 = createEggId(eggIdShell)
            val eggId2 = createEggId(sameEggIdShell)

            // when
            val actual = eggId1.equals(eggId2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to eggId with other eggIdShell`() {
            // given
            val eggIdShell = shellOf("abc")
            val otherEggIdShell = shellOf("abd")
            val eggId1 = createEggId(eggIdShell)
            val eggId2 = createEggId(otherEggIdShell)

            // when
            val actual = eggId1.equals(eggId2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val eggIdShell = shellOf("abc")
            val eggId = createEggId(eggIdShell)

            // when
            val actual = eggId.equals(eggIdShell)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val eggId = createEggId(shellOf("abc"))

            // when
            val actual = eggId.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
