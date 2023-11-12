package de.pflugradts.passbird.domain.model.namespace

import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf

class Namespace private constructor(val bytes: Bytes, val slot: NamespaceSlot) {
    override fun toString() = "Namespace(slot=$slot)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Namespace
        return slot == other.slot
    }
    override fun hashCode() = slot.hashCode()
    companion object {
        val DEFAULT = Namespace(bytesOf("Default"), NamespaceSlot.DEFAULT)
        fun createNamespace(bytes: Bytes, namespaceSlot: NamespaceSlot) = Namespace(bytes.copy(), namespaceSlot)
    }
}
