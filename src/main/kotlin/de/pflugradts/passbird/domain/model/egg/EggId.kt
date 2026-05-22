package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.shell.EncryptedShell

class EggId private constructor(private var encryptedShell: EncryptedShell) : DomainEntity {
    fun rename(newEncryptedShell: EncryptedShell) {
        encryptedShell = newEncryptedShell.copy()
    }
    fun view() = encryptedShell.copy()
    override fun equals(other: Any?) = (other as? EggId)?.view() == encryptedShell
    override fun hashCode() = encryptedShell.hashCode()
    companion object {
        fun createEggId(shell: EncryptedShell) = EggId(shell.copy())
    }
}
