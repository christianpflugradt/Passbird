package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.shell.Shell

class EggId private constructor(private var shell: Shell) : DomainEntity {
    fun rename(newShell: Shell) {
        shell = newShell.copy()
    }
    fun view() = shell.copy()
    override fun equals(other: Any?) = (other as? EggId)?.view() == shell
    override fun hashCode() = shell.hashCode()
    companion object {
        fun createEggId(shell: Shell) = EggId(shell.copy())
    }
}
