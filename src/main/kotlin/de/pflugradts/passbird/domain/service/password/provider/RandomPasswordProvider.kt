package de.pflugradts.passbird.domain.service.password.provider

import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.shell.MAX_ASCII_VALUE
import de.pflugradts.passbird.domain.model.shell.MIN_ASCII_VALUE
import de.pflugradts.passbird.domain.model.shell.PlainValue
import de.pflugradts.passbird.domain.model.shell.PlainValue.Companion.plainValueOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.security.SecureRandom
import kotlin.reflect.KProperty1

private val random = SecureRandom()

class RandomPasswordProvider : PasswordProvider {
    override fun createNewPassword(passwordRequirements: PasswordRequirements): Shell {
        var passwordShell = emptyShell()
        while (!isStrong(passwordShell, passwordRequirements)) { passwordShell = randomPassword(passwordRequirements) }
        return passwordShell
    }

    private fun randomPassword(passwordRequirements: PasswordRequirements) =
        shellOf((0..<passwordRequirements.length).map { randomByte(passwordRequirements) }.toList())

    private fun randomByte(passwordRequirements: PasswordRequirements): Byte {
        val getRandom = { (random.nextInt(MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toByte() }
        var result: Byte
        do { result = getRandom() } while (!result.satisfies(passwordRequirements))
        return result
    }

    private fun isStrong(passwordShell: Shell, passwordRequirements: PasswordRequirements) =
        (!passwordRequirements.hasNumbers || passwordShell.anyMatch(PlainValue::isDigit)) &&
            (!passwordRequirements.hasUppercaseLetters || passwordShell.anyMatch(PlainValue::isUppercaseCharacter)) &&
            (!passwordRequirements.hasLowercaseLetters || passwordShell.anyMatch(PlainValue::isLowercaseCharacter)) &&
            (!passwordRequirements.hasSpecialCharacters || passwordShell.anyMatch(PlainValue::isSymbol))

    private fun Shell.anyMatch(property: KProperty1<PlainValue, Boolean>) = copy().stream().anyMatch { property.get(plainValueOf(it)) }
    private fun Byte.matches(property: KProperty1<PlainValue, Boolean>) = property.get(plainValueOf(this))
    private fun Byte.satisfies(passwordRequirements: PasswordRequirements): Boolean {
        if (passwordRequirements.hasSpecialCharacters && this.toInt().toChar() in passwordRequirements.unusedSpecialCharacters) return false
        if (!passwordRequirements.hasSpecialCharacters && matches(PlainValue::isSymbol)) return false
        if (!passwordRequirements.hasNumbers && passwordRequirements.isValid() && matches(PlainValue::isDigit)) return false
        if (!passwordRequirements.hasLowercaseLetters && matches(PlainValue::isLowercaseCharacter)) return false
        if (!passwordRequirements.hasUppercaseLetters && matches(PlainValue::isUppercaseCharacter)) return false
        return true
    }
}
