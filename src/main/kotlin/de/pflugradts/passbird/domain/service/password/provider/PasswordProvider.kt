package de.pflugradts.passbird.domain.service.password.provider

import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.shell.Shell

interface PasswordProvider {
    fun createNewPassword(passwordRequirements: PasswordRequirements): Shell
}
