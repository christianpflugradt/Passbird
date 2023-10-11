package de.pflugradts.passbird.domain.model.namespace

import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf

class Namespace private constructor(val bytes: Bytes, val slot: NamespaceSlot) {
    companion object {
        val DEFAULT = Namespace(bytesOf("Default"), NamespaceSlot.DEFAULT)
        fun createNamespace(bytes: Bytes, namespaceSlot: NamespaceSlot) = Namespace(bytes.copy(), namespaceSlot)
    }
}
