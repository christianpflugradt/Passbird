package de.pflugradts.passbird.domain.model.namespace

import de.pflugradts.passbird.domain.model.namespace.Namespace.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.namespace.Namespace.Companion.createNamespace
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.FIRST_NAMESPACE
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.LAST_NAMESPACE
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.at
import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.Collections
import java.util.Optional

class Namespaces {
    private val namespaces = Collections.nCopies(CAPACITY, Optional.empty<Namespace>()).toMutableList()
    private var currentNamespace = NamespaceSlot.DEFAULT

    fun reset() = (0..<CAPACITY).forEach { namespaces[it] = Optional.empty() }

    fun populate(namespaceBytes: List<Bytes>) {
        if (namespaces.find { it.isPresent } == null && namespaceBytes.size == NamespaceSlot.CAPACITY) {
            (FIRST_NAMESPACE..LAST_NAMESPACE).forEach {
                if (namespaceBytes[it - 1].isNotEmpty) {
                    namespaces[it - 1] = Optional.of(createNamespace(namespaceBytes[it - 1], at(it)))
                }
            }
        }
    }

    fun deploy(namespaceBytes: Bytes, namespaceSlot: NamespaceSlot) {
        namespaces[namespaceSlot.index() - 1] = Optional.of(createNamespace(namespaceBytes, namespaceSlot))
    }
    fun atSlot(namespaceSlot: NamespaceSlot): Optional<Namespace> =
        if (namespaceSlot === NamespaceSlot.DEFAULT) Optional.of(DEFAULT) else namespaces[namespaceSlot.index() - 1]
    fun all() = namespaces.stream()
    fun getCurrentNamespace() = atSlot(currentNamespace).orElse(DEFAULT)
    fun updateCurrentNamespace(namespaceSlot: NamespaceSlot) { if (atSlot(namespaceSlot).isPresent) { currentNamespace = namespaceSlot } }
}
