package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.transfer.Bytes

class Password private constructor(private var bytes: Bytes) : DomainEntity {
    fun update(newBytes: Bytes) { bytes = newBytes.copy() }
    fun discard() { bytes.scramble() }
    fun view() = bytes.copy()
    override fun equals(other: Any?) = (other as? Password)?.view() == bytes
    override fun hashCode() = bytes.hashCode()
    companion object {
        fun createPassword(bytes: Bytes) = Password(bytes.copy())
    }
}
