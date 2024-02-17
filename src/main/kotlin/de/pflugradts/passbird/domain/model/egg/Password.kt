package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.shell.Shell

class Password private constructor(private var shell: Shell) : DomainEntity {
    fun update(newShell: Shell) {
        shell = newShell.copy()
    }
    fun discard() = shell.scramble()
    fun view() = shell.copy()
    override fun equals(other: Any?) = (other as? Password)?.view() == shell
    override fun hashCode() = shell.hashCode()
    companion object {
        fun createPassword(shell: Shell) = Password(shell.copy())
    }
}
