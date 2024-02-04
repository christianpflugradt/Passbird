package de.pflugradts.passbird.domain.service.password.provider

import de.pflugradts.passbird.domain.model.egg.createPasswordRequirementsForTesting
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
        val passwordRequirements = createPasswordRequirementsForTesting(withLength = passwordLength)

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
        val passwordRequirements = createPasswordRequirementsForTesting()

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches ".*[0-9].*".toRegex()
        }
    }

    @Test
    fun `should include uppercase`() {
        // given
        val passwordRequirements = createPasswordRequirementsForTesting()

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches ".*[A-Z].*".toRegex()
        }
    }

    @Test
    fun `should include lowercase`() {
        // given
        val passwordRequirements = createPasswordRequirementsForTesting()

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches ".*[a-z].*".toRegex()
        }
    }

    @Test
    fun `should include special characters if enabled`() {
        // given
        val passwordRequirements = createPasswordRequirementsForTesting(withSpecialCharacters = true)

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
        val passwordRequirements = createPasswordRequirementsForTesting(withSpecialCharacters = false)

        // when
        val createPassword = { passwordProvider.createNewPassword(passwordRequirements) }

        // then
        expectManyTimes {
            expectThat(createPassword().asString()) matches "^[0-9A-Za-z]+\$".toRegex()
        }
    }

    private fun expectManyTimes(block: () -> Unit) { repeat((1..100).count()) { block() } }
}
