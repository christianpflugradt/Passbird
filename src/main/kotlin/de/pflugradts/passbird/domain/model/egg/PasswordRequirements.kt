package de.pflugradts.passbird.domain.model.egg

class PasswordRequirements(
    val length: Int,
    val hasNumbers: Boolean = true,
    val hasLowercaseLetters: Boolean = true,
    val hasUppercaseLetters: Boolean = true,
    val hasSpecialCharacters: Boolean = true,
    val unusedSpecialCharacters: String = "",
) {
    fun isValid() = length > 0 &&
        (hasNumbers || hasLowercaseLetters || hasUppercaseLetters || hasSpecialCharacters) &&
        (hasNumbers || hasLowercaseLetters || hasUppercaseLetters || unusedSpecialCharacters.length <= 20)

}
