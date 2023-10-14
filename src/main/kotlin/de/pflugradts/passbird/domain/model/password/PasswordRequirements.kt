package de.pflugradts.passbird.domain.model.password

class PasswordRequirements private constructor(val includeSpecialCharacters: Boolean, val passwordLength: Int) {
    companion object {
        fun passwordRequirementsOf(includeSpecialCharacters: Boolean, passwordLength: Int) =
            PasswordRequirements(includeSpecialCharacters, passwordLength)
    }
}
