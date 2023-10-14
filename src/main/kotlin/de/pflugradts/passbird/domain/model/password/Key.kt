package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.transfer.Bytes

/**
 * A Key identifies a [Password].
 */
class Key private constructor(private var bytes: Bytes) : DomainEntity {
    fun rename(newBytes: Bytes) { bytes = newBytes.copy() }
    fun view() = bytes.copy()
    override fun equals(other: Any?) = (other as? Key)?.view() == bytes
    override fun hashCode() = bytes.hashCode()
    companion object {
        fun createKey(bytes: Bytes) = Key(bytes.copy())
    }
}
