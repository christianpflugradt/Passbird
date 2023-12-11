package de.pflugradts.passbird.domain.model.egg

class PasswordRequirements private constructor(val includeSpecialCharacters: Boolean, val passwordLength: Int) {
    companion object {
        fun passwordRequirementsOf(includeSpecialCharacters: Boolean, passwordLength: Int) =
            PasswordRequirements(includeSpecialCharacters, passwordLength)
    }
}
