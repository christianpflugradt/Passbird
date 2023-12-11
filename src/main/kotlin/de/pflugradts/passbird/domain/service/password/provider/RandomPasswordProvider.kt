package de.pflugradts.passbird.domain.service.password.provider

import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.CharValue
import de.pflugradts.passbird.domain.model.transfer.CharValue.Companion.charValueOf
import de.pflugradts.passbird.domain.model.transfer.MAX_ASCII_VALUE
import de.pflugradts.passbird.domain.model.transfer.MIN_ASCII_VALUE
import java.security.SecureRandom
import kotlin.reflect.KProperty1

private val random = SecureRandom()

class RandomPasswordProvider : PasswordProvider {
    override fun createNewPassword(passwordRequirements: PasswordRequirements): Bytes {
        var passwordBytes = emptyBytes()
        while (!isStrong(passwordBytes, passwordRequirements)) {
            passwordBytes = randomPassword(passwordRequirements)
        }
        return passwordBytes
    }

    private fun randomPassword(passwordRequirements: PasswordRequirements) =
        bytesOf((0..<passwordRequirements.passwordLength).map { randomByte(!passwordRequirements.includeSpecialCharacters) }.toList())

    private fun randomByte(avoidSymbols: Boolean): Byte {
        val getRandom = { (random.nextInt(MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toByte() }
        var result: Byte
        do { result = getRandom() } while (avoidSymbols && charValueOf(result).isSymbol)
        return result
    }

    private fun isStrong(passwordBytes: Bytes, requirements: PasswordRequirements) =
        passwordBytes.anyMatch(CharValue::isDigit) &&
            passwordBytes.anyMatch(CharValue::isUppercaseCharacter) &&
            passwordBytes.anyMatch(CharValue::isLowercaseCharacter) &&
            (!requirements.includeSpecialCharacters || passwordBytes.anyMatch(CharValue::isSymbol))

    private fun Bytes.anyMatch(property: KProperty1<CharValue, Boolean>) = copy().stream().anyMatch { property.get(charValueOf(it)) }
}
