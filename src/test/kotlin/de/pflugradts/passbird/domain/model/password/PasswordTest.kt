package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.password.Key.Companion.createKey
import de.pflugradts.passbird.domain.model.password.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class PasswordTest {
    @Test
    fun `should CreatePassword`() {
        // given
        val bytes = bytesOf("password")

        // when
        val actual = createPassword(bytes)

        // then
        expectThat(actual.view()) isEqualTo bytes
    }

    @Test
    fun `should update password`() {
        // given
        val originalBytes = bytesOf("password")
        val password = createPassword(originalBytes)
        val updatedBytes = bytesOf("p4s5w0rD")

        // when
        password.update(updatedBytes)
        val actual = password.view()

        // then
        expectThat(actual) isEqualTo updatedBytes isNotEqualTo originalBytes
    }

    @Test
    fun `should discard password`() {
        // given
        val givenBytes = mockk<Bytes>(relaxed = true)
        every { givenBytes.copy() } returns givenBytes
        val password = createPassword(givenBytes)

        // when
        password.discard()

        // then
        verify { givenBytes.scramble() }
    }

    @Test
    fun `should clone bytes on creation`() {
        // given
        val bytes = bytesOf("password")
        val password = createPassword(bytes)

        // when
        bytes.scramble()
        val actual = password.view()

        // then
        expectThat(actual) isNotEqualTo bytes
    }

    @Test
    fun `should CloneBytesOnUpdate`() {
        // given
        val originalBytes = bytesOf("password")
        val password = createPassword(originalBytes)
        val updatedBytes = bytesOf("p4s5w0rD")

        // when
        password.update(updatedBytes)
        updatedBytes.scramble()
        val actual = password.view()

        // then
        expectThat(actual) isNotEqualTo updatedBytes
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to key with equal bytes`() {
            // given
            val bytes = bytesOf("password")
            val password1 = createPassword(bytes)
            val password2 = createPassword(bytes)

            // when
            val actual = password1.equals(password2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to key with other bytes`() {
            // given
            val bytes = bytesOf("password")
            val otherBytes = bytesOf("other")
            val password1 = createPassword(bytes)
            val password2 = createPassword(otherBytes)

            // when
            val actual = password1.equals(password2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val bytes = bytesOf("password")
            val password = createPassword(bytes)

            // when
            val actual = password.equals(createKey(bytes))

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val password = createPassword(bytesOf("password"))

            // when
            val actual = password.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
