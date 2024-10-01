package de.pflugradts.passbird.domain.service.password.encryption

import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell

interface CryptoProvider {
    fun encrypt(shell: Shell): EncryptedShell
    fun decrypt(encryptedShell: EncryptedShell): Shell
}
