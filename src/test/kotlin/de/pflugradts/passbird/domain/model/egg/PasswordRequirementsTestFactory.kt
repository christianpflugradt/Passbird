package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.PasswordRequirements.Companion.passwordRequirementsOf

fun createPasswordRequirementsForTesting(
    withIncludeSpecialCharacters: Boolean = true,
    withPasswordLength: Int = 10,
): PasswordRequirements = passwordRequirementsOf(withIncludeSpecialCharacters, withPasswordLength)
