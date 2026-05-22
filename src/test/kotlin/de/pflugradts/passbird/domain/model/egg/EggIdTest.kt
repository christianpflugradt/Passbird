package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
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
        val eggIdShell = shellOf("EggId")

        // when
        val eggId = createEggId(eggIdShell.fakeEnc())

        // then
        expectThat(eggId.view().fakeDec()) isEqualTo eggIdShell
    }

    @Test
    fun `should rename eggId`() {
        // given
        val originalEggIdShell = shellOf("EggId123")
        val eggId = createEggId(originalEggIdShell.fakeEnc())
        val updatedEggIdShell = shellOf("EggIdABC")

        // when
        eggId.rename(updatedEggIdShell.fakeEnc())
        val actual = eggId.view()

        // then
        expectThat(actual.fakeDec()) isEqualTo updatedEggIdShell isNotEqualTo originalEggIdShell
    }

    @Test
    fun `should clone eggIdShell`() {
        // given
        val eggIdShell = shellOf("EggId")
        val eggId = createEggId(eggIdShell.fakeEnc())

        // when
        eggIdShell.scramble()
        val actual = eggId.view()

        // then
        expectThat(actual) isNotEqualTo eggIdShell.fakeEnc()
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val eggId1 = createEggId(shellOf("abc").fakeEnc())
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
            val eggId1 = createEggId(eggIdShell.fakeEnc())
            val eggId2 = createEggId(sameEggIdShell.fakeEnc())

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
            val eggId1 = createEggId(eggIdShell.fakeEnc())
            val eggId2 = createEggId(otherEggIdShell.fakeEnc())

            // when
            val actual = eggId1.equals(eggId2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val eggIdShell = shellOf("abc")
            val eggId = createEggId(eggIdShell.fakeEnc())

            // when
            val actual = eggId.equals(eggIdShell)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val eggId = createEggId(shellOf("abc").fakeEnc())

            // when
            val actual = eggId.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
