package de.pflugradts.passbird.domain.service.password.encryption

import de.pflugradts.passbird.domain.model.shell.Shell

interface CryptoProvider {
    fun encrypt(shell: Shell): Shell
    fun decrypt(shell: Shell): Shell
}
