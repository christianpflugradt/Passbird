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
        while (!isStrong(passwordShell, passwordRequirements)) {
            passwordShell = randomPassword(passwordRequirements)
        }
        return passwordShell
    }

    private fun randomPassword(passwordRequirements: PasswordRequirements) =
        shellOf((0..<passwordRequirements.passwordLength).map { randomByte(!passwordRequirements.includeSpecialCharacters) }.toList())

    private fun randomByte(avoidSymbols: Boolean): Byte {
        val getRandom = { (random.nextInt(MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toByte() }
        var result: Byte
        do { result = getRandom() } while (avoidSymbols && plainValueOf(result).isSymbol)
        return result
    }

    private fun isStrong(passwordShell: Shell, requirements: PasswordRequirements) =
        passwordShell.anyMatch(PlainValue::isDigit) &&
            passwordShell.anyMatch(PlainValue::isUppercaseCharacter) &&
            passwordShell.anyMatch(PlainValue::isLowercaseCharacter) &&
            (!requirements.includeSpecialCharacters || passwordShell.anyMatch(PlainValue::isSymbol))

    private fun Shell.anyMatch(property: KProperty1<PlainValue, Boolean>) = copy().stream().anyMatch { property.get(plainValueOf(it)) }
}
