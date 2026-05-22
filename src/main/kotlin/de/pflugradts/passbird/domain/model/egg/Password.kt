package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.shell.EncryptedShell

class Password private constructor(private var encryptedShell: EncryptedShell) : DomainEntity {
    fun update(newEncryptedShell: EncryptedShell) {
        encryptedShell = newEncryptedShell.copy()
    }
    fun discard() = encryptedShell.scramble()
    fun view() = encryptedShell.copy()
    override fun equals(other: Any?) = (other as? Password)?.view() == encryptedShell
    override fun hashCode() = encryptedShell.hashCode()
    companion object {
        fun createPassword(encryptedShell: EncryptedShell) = Password(encryptedShell.copy())
    }
}
