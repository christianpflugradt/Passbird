package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
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
        val passwordShell = shellOf("password")

        // when
        val actual = createPassword(passwordShell)

        // then
        expectThat(actual.view()) isEqualTo passwordShell
    }

    @Test
    fun `should update password`() {
        // given
        val originalPasswordShell = shellOf("password")
        val password = createPassword(originalPasswordShell)
        val updatedPasswordShell = shellOf("p4s5w0rD")

        // when
        password.update(updatedPasswordShell)
        val actual = password.view()

        // then
        expectThat(actual) isEqualTo updatedPasswordShell isNotEqualTo originalPasswordShell
    }

    @Test
    fun `should discard password`() {
        // given
        val givenShell = mockk<Shell>(relaxed = true)
        every { givenShell.copy() } returns givenShell
        val password = createPassword(givenShell)

        // when
        password.discard()

        // then
        verify { givenShell.scramble() }
    }

    @Test
    fun `should clone passwordShell on creation`() {
        // given
        val passwordShell = shellOf("password")
        val password = createPassword(passwordShell)

        // when
        passwordShell.scramble()
        val actual = password.view()

        // then
        expectThat(actual) isNotEqualTo passwordShell
    }

    @Test
    fun `should clone passwordShell on update`() {
        // given
        val originalPasswordShell = shellOf("password")
        val password = createPassword(originalPasswordShell)
        val updatedPasswordShell = shellOf("p4s5w0rD")

        // when
        password.update(updatedPasswordShell)
        updatedPasswordShell.scramble()
        val actual = password.view()

        // then
        expectThat(actual) isNotEqualTo updatedPasswordShell
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val password1 = createPassword(shellOf("abc"))
            val password2 = password1

            // when
            val actual = password1.equals(password2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to password with equal passwordShell`() {
            // given
            val passwordShell = shellOf("abc")
            val samePasswordShell = shellOf("abc")
            val password1 = createPassword(passwordShell)
            val password2 = createPassword(samePasswordShell)

            // when
            val actual = password1.equals(password2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to password with other passwordShell`() {
            // given
            val passwordShell = shellOf("abc")
            val otherPasswordShell = shellOf("abd")
            val password1 = createPassword(passwordShell)
            val password2 = createPassword(otherPasswordShell)

            // when
            val actual = password1.equals(password2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val passwordShell = shellOf("abc")
            val password = createPassword(passwordShell)

            // when
            val actual = password.equals(passwordShell)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val password = createPassword(shellOf("abc"))

            // when
            val actual = password.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
