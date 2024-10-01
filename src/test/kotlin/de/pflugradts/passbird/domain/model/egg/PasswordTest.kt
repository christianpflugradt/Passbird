package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
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
        val passwordShell = shellOf("Password")

        // when
        val actual = createPassword(passwordShell.fakeEnc())

        // then
        expectThat(actual.view().fakeDec()) isEqualTo passwordShell
    }

    @Test
    fun `should update password`() {
        // given
        val originalPasswordShell = shellOf("Password")
        val password = createPassword(originalPasswordShell.fakeEnc())
        val updatedPasswordShell = shellOf("p4s5w0rD")

        // when
        password.update(updatedPasswordShell.fakeEnc())
        val actual = password.view()

        // then
        expectThat(actual.fakeDec()) isEqualTo updatedPasswordShell isNotEqualTo originalPasswordShell
    }

    @Test
    fun `should discard password`() {
        // given
        val givenShell = mockk<Shell>(relaxed = true)
        every { givenShell.copy() } returns givenShell
        val password = createPassword(givenShell.fakeEnc())

        // when
        password.discard()

        // then
        verify { givenShell.scramble() }
    }

    @Test
    fun `should clone passwordShell on creation`() {
        // given
        val passwordShell = shellOf("Password")
        val password = createPassword(passwordShell.fakeEnc())

        // when
        passwordShell.scramble()
        val actual = password.view()

        // then
        expectThat(actual) isNotEqualTo passwordShell.fakeEnc()
    }

    @Test
    fun `should clone passwordShell on update`() {
        // given
        val originalPasswordShell = shellOf("Password")
        val password = createPassword(originalPasswordShell.fakeEnc())
        val updatedPasswordShell = shellOf("p4s5w0rD")

        // when
        password.update(updatedPasswordShell.fakeEnc())
        updatedPasswordShell.scramble()
        val actual = password.view()

        // then
        expectThat(actual) isNotEqualTo updatedPasswordShell.fakeEnc()
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val password1 = createPassword(shellOf("abc").fakeEnc())
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
            val password1 = createPassword(passwordShell.fakeEnc())
            val password2 = createPassword(samePasswordShell.fakeEnc())

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
            val password1 = createPassword(passwordShell.fakeEnc())
            val password2 = createPassword(otherPasswordShell.fakeEnc())

            // when
            val actual = password1.equals(password2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val passwordShell = shellOf("abc")
            val password = createPassword(passwordShell.fakeEnc())

            // when
            val actual = password.equals(passwordShell)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val password = createPassword(shellOf("abc").fakeEnc())

            // when
            val actual = password.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
