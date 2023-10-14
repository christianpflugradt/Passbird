package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.password.Key.Companion.createKey
import de.pflugradts.passbird.domain.model.password.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class KeyTest {

    @Test
    fun `should create key`() {
        // given
        val bytes = bytesOf("key")

        // when
        val key = createKey(bytes)

        // then
        expectThat(key.view()) isEqualTo bytes
    }

    @Test
    fun `should rename key`() {
        // given
        val originalBytes = bytesOf("key123")
        val key = createKey(originalBytes)
        val updatedBytes = bytesOf("keyABC")

        // when
        key.rename(updatedBytes)
        val actual = key.view()

        // then
        expectThat(actual) isEqualTo updatedBytes isNotEqualTo originalBytes
    }

    @Test
    fun `should clone bytes`() {
        // given
        val bytes = bytesOf("key")
        val key = createKey(bytes)

        // when
        bytes.scramble()
        val actual = key.view()

        // then
        expectThat(actual) isNotEqualTo bytes
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to key with equal bytes`() {
            // given
            val bytes = bytesOf("key")
            val key1 = createKey(bytes)
            val key2 = createKey(bytes)

            // when
            val actual = key1.equals(key2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to key with other bytes`() {
            // given
            val bytes = bytesOf("key")
            val otherBytes = bytesOf("other")
            val key1 = createKey(bytes)
            val key2 = createKey(otherBytes)

            // when
            val actual = key1.equals(key2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val bytes = bytesOf("key")
            val key = createKey(bytes)

            // when
            val actual = key.equals(createPassword(bytes))

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val key = createKey(bytesOf("key"))

            // when
            val actual = key.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}