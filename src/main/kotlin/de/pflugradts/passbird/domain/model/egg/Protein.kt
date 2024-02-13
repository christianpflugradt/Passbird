package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.shell.Shell

class Protein private constructor(private var type: Shell, private var structure: Shell) : DomainEntity {
    fun viewType() = type.copy()
    fun viewStructure() = structure.copy()
    override fun equals(other: Any?) =
        (other as? Protein)?.let { it.viewType() == viewType() && it.viewStructure() == viewStructure() } ?: false
    override fun hashCode() = viewStructure().hashCode() + 31 * viewType().hashCode()
    companion object {
        fun createProtein(type: Shell, structure: Shell) = Protein(type.copy(), structure.copy())
    }
}
