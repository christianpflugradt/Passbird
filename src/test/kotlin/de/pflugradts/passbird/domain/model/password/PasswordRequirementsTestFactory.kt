package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.password.PasswordRequirements.Companion.passwordRequirementsOf

fun createPasswordRequirementsForTesting(
    withIncludeSpecialCharacters: Boolean = true,
    withPasswordLength: Int = 10,
): PasswordRequirements = passwordRequirementsOf(withIncludeSpecialCharacters, withPasswordLength)
