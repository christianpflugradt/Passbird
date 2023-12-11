package de.pflugradts.passbird.domain.service.password.provider

import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.transfer.Bytes

/**
 * A PasswordProvider generates new Passwords.
 */
interface PasswordProvider {
    fun createNewPassword(passwordRequirements: PasswordRequirements): Bytes
}
