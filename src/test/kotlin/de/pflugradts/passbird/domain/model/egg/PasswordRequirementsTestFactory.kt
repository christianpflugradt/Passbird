package de.pflugradts.passbird.domain.model.egg

fun createPasswordRequirementsForTesting(
    withSpecialCharacters: Boolean = true,
    withLength: Int = 10,
) = PasswordRequirements(length = withLength, hasSpecialCharacters = withSpecialCharacters)
