package de.pflugradts.passbird.domain.model.egg

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class PasswordRequirementsTest {

    @Test
    fun `requirements with positive length should be valid`() {
        // given
        val givenPasswordRequirements = PasswordRequirements(
            length = 1,
            hasNumbers = true,
            hasLowercaseLetters = true,
            hasUppercaseLetters = true,
            hasSpecialCharacters = true,
            unusedSpecialCharacters = "",
        )

        // when
        val actual = givenPasswordRequirements.isValid()

        // then
        expectThat(actual).isTrue()
    }

    @Test
    fun `requirements with only numbers should be valid`() {
        // given
        val givenPasswordRequirements = PasswordRequirements(
            length = 20,
            hasNumbers = true,
            hasLowercaseLetters = false,
            hasUppercaseLetters = false,
            hasSpecialCharacters = false,
            unusedSpecialCharacters = "",
        )

        // when
        val actual = givenPasswordRequirements.isValid()

        // then
        expectThat(actual).isTrue()
    }

    @Test
    fun `requirements with only lowercase should be valid`() {
        // given
        val givenPasswordRequirements = PasswordRequirements(
            length = 20,
            hasNumbers = false,
            hasLowercaseLetters = true,
            hasUppercaseLetters = false,
            hasSpecialCharacters = false,
            unusedSpecialCharacters = "",
        )

        // when
        val actual = givenPasswordRequirements.isValid()

        // then
        expectThat(actual).isTrue()
    }

    @Test
    fun `requirements with only uppercase should be valid`() {
        // given
        val givenPasswordRequirements = PasswordRequirements(
            length = 20,
            hasNumbers = false,
            hasLowercaseLetters = false,
            hasUppercaseLetters = true,
            hasSpecialCharacters = false,
            unusedSpecialCharacters = "",
        )

        // when
        val actual = givenPasswordRequirements.isValid()

        // then
        expectThat(actual).isTrue()
    }

    @Test
    fun `requirements with only special characters should be valid`() {
        // given
        val givenPasswordRequirements = PasswordRequirements(
            length = 20,
            hasNumbers = false,
            hasLowercaseLetters = false,
            hasUppercaseLetters = false,
            hasSpecialCharacters = true,
            unusedSpecialCharacters = "",
        )

        // when
        val actual = givenPasswordRequirements.isValid()

        // then
        expectThat(actual).isTrue()
    }

    @Test
    fun `requirements with only special characters and up to 20 unused special characters should be valid`() {
        // given
        (1..20).forEach {
            val givenPasswordRequirements = PasswordRequirements(
                length = 20,
                hasNumbers = false,
                hasLowercaseLetters = false,
                hasUppercaseLetters = false,
                hasSpecialCharacters = true,
                unusedSpecialCharacters = "$".repeat(it),
            )

            // when
            val actual = givenPasswordRequirements.isValid()

            // then
            expectThat(actual).isTrue()
        }
    }

    @Test
    fun `requirements with only special characters and more than 20 unused special characters should be invalid`() {
        // given
        val givenPasswordRequirements = PasswordRequirements(
            length = 20,
            hasNumbers = false,
            hasLowercaseLetters = false,
            hasUppercaseLetters = false,
            hasSpecialCharacters = true,
            unusedSpecialCharacters = "$".repeat(21),
        )

        // when
        val actual = givenPasswordRequirements.isValid()

        // then
        expectThat(actual).isFalse()
    }

    @ParameterizedTest
    @ValueSource(
        ints = [
            0,
            -1,
        ],
    )
    fun `requirements with invalid length should not be valid`(length: Int) {
        // given
        val givenPasswordRequirements = PasswordRequirements(
            length = length,
            hasNumbers = true,
            hasLowercaseLetters = true,
            hasUppercaseLetters = true,
            hasSpecialCharacters = true,
            unusedSpecialCharacters = "",
        )

        // when
        val actual = givenPasswordRequirements.isValid()

        // then
        expectThat(actual).isFalse()
    }

    @Test
    fun `requirements without any character types should not be valid`() {
        // given
        val givenPasswordRequirements = PasswordRequirements(
            length = 20,
            hasNumbers = false,
            hasLowercaseLetters = false,
            hasUppercaseLetters = false,
            hasSpecialCharacters = false,
            unusedSpecialCharacters = "",
        )

        // when
        val actual = givenPasswordRequirements.isValid()

        // then
        expectThat(actual).isFalse()
    }
}
