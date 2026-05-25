package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.shell.MAX_ASCII_VALUE
import de.pflugradts.passbird.domain.model.shell.MIN_ASCII_VALUE
import de.pflugradts.passbird.domain.model.shell.PlainValue.Companion.plainValueOf

class PasswordRequirements(
    val length: Int,
    val hasNumbers: Boolean = true,
    val hasLowercaseLetters: Boolean = true,
    val hasUppercaseLetters: Boolean = true,
    val hasSpecialCharacters: Boolean = true,
    val unusedSpecialCharacters: String = "",
) {
    fun isValid() = length > 0 &&
        length >= requiredCharacterTypeCount() &&
        (hasNumbers || hasLowercaseLetters || hasUppercaseLetters || hasSpecialCharacters) &&
        (hasNumbers || hasLowercaseLetters || hasUppercaseLetters || unusedSpecialCharacters.length <= 20) &&
        (!hasSpecialCharacters || availableSpecialCharacters().isNotEmpty())

    private fun requiredCharacterTypeCount() = listOf(
        hasNumbers,
        hasLowercaseLetters,
        hasUppercaseLetters,
        hasSpecialCharacters,
    ).count { it }

    private fun availableSpecialCharacters() = (MIN_ASCII_VALUE until MAX_ASCII_VALUE)
        .map { it.toChar() }
        .filter { plainValueOf(it).isSymbol }
        .filter { it !in unusedSpecialCharacters }
}
