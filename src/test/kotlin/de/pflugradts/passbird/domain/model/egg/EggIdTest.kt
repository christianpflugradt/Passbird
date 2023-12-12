package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
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
        val bytes = bytesOf("eggId")

        // when
        val eggId = createEggId(bytes)

        // then
        expectThat(eggId.view()) isEqualTo bytes
    }

    @Test
    fun `should rename eggId`() {
        // given
        val originalBytes = bytesOf("eggId123")
        val eggId = createEggId(originalBytes)
        val updatedBytes = bytesOf("eggIdABC")

        // when
        eggId.rename(updatedBytes)
        val actual = eggId.view()

        // then
        expectThat(actual) isEqualTo updatedBytes isNotEqualTo originalBytes
    }

    @Test
    fun `should clone bytes`() {
        // given
        val bytes = bytesOf("eggId")
        val eggId = createEggId(bytes)

        // when
        bytes.scramble()
        val actual = eggId.view()

        // then
        expectThat(actual) isNotEqualTo bytes
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val eggId1 = createEggId(bytesOf("abc"))
            val eggId2 = eggId1

            // when
            val actual = eggId1.equals(eggId2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to eggId with equal bytes`() {
            // given
            val bytes = bytesOf("abc")
            val sameBytes = bytesOf("abc")
            val eggId1 = createEggId(bytes)
            val eggId2 = createEggId(sameBytes)

            // when
            val actual = eggId1.equals(eggId2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to eggId with other bytes`() {
            // given
            val bytes = bytesOf("abc")
            val otherBytes = bytesOf("abd")
            val eggId1 = createEggId(bytes)
            val eggId2 = createEggId(otherBytes)

            // when
            val actual = eggId1.equals(eggId2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val bytes = bytesOf("abc")
            val eggId = createEggId(bytes)

            // when
            val actual = eggId.equals(bytes)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val eggId = createEggId(bytesOf("abc"))

            // when
            val actual = eggId.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
