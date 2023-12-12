package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.transfer.Bytes

class EggId private constructor(private var bytes: Bytes) : DomainEntity {
    fun rename(newBytes: Bytes) { bytes = newBytes.copy() }
    fun view() = bytes.copy()
    override fun equals(other: Any?) = (other as? EggId)?.view() == bytes
    override fun hashCode() = bytes.hashCode()
    companion object {
        fun createEggId(bytes: Bytes) = EggId(bytes.copy())
    }
}
