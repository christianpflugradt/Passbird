package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.shell.EncryptedShell

class Protein private constructor(private var type: EncryptedShell, private var structure: EncryptedShell) : DomainEntity {
    fun viewType() = type.copy()
    fun viewStructure() = structure.copy()
    override fun equals(other: Any?) =
        (other as? Protein)?.let { it.viewType() == viewType() && it.viewStructure() == viewStructure() } ?: false
    override fun hashCode() = viewStructure().hashCode() + 31 * viewType().hashCode()
    companion object {
        fun createProtein(type: EncryptedShell, structure: EncryptedShell) = Protein(type.copy(), structure.copy())
    }
}
