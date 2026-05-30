package de.pflugradts.passbird.domain.service.password.provider

import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.matches

class RandomPasswordProviderTest {

    private val passwordProvider = RandomPasswordProvider()

    @Test
    fun `should use given length`() {
        // given
        val passwordLength = 25
        val passwordRequirements = PasswordRequirements(length = passwordLength)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().toByteArray().size) isEqualTo passwordLength
        }
    }

    @Test
    fun `should include digits`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches ".*[0-9].*".toRegex()
        }
    }

    @Test
    fun `should not include digits if disabled`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20, hasNumbers = false)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches "^[^0-9]+\$".toRegex()
        }
    }

    @Test
    fun `should include uppercase`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches ".*[A-Z].*".toRegex()
        }
    }

    @Test
    fun `should not include uppercase if disabled`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20, hasUppercaseLetters = false)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches "^[^A-Z]+\$".toRegex()
        }
    }

    @Test
    fun `should include lowercase`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches ".*[a-z].*".toRegex()
        }
    }

    @Test
    fun `should not include lowercase if disabled`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20, hasLowercaseLetters = false)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches "^[^a-z]+\$".toRegex()
        }
    }

    @Test
    fun `should include special characters`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches ".*[^0-9A-Za-z].*".toRegex()
        }
    }

    @Test
    fun `should not include special characters if disabled`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20, hasSpecialCharacters = false)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches "^[0-9A-Za-z]+\$".toRegex()
        }
    }

    @Test
    fun `should not include unused if disabled`() {
        // given
        val passwordRequirements = PasswordRequirements(length = 20, hasSpecialCharacters = true, unusedSpecialCharacters = " +#(='")

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches "^[^ +#(=']+\$".toRegex()
        }
    }

    @Test
    fun `should scrub rejected candidates without copying strength checks`() {
        // given
        val rejectedPassword = spyk(shellOf("aaaa"))
        val acceptedPassword = spyk(shellOf("aA1!"))
        val candidates = listOf(rejectedPassword, acceptedPassword).iterator()
        val passwordProvider = RandomPasswordProvider { candidates.next() }
        val passwordRequirements = PasswordRequirements(length = 4)

        // when
        val actual = passwordProvider.createNewPassword(passwordRequirements)

        // then
        expectThat(actual) isEqualTo acceptedPassword
        verify(exactly = 1) { rejectedPassword.scramble() }
        verify(exactly = 0) { acceptedPassword.scramble() }
        verify(exactly = 0) { rejectedPassword.copy() }
        verify(exactly = 0) { acceptedPassword.copy() }
    }

    private fun expectManyTimes(block: () -> Unit) {
        repeat((1..100).count()) { block() }
    }
}
