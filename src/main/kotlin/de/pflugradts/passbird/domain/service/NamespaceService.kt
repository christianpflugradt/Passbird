package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.namespace.Namespace
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.Optional
import java.util.stream.Stream

interface NamespaceService {
    fun populate(namespaceBytes: List<Bytes>)
    fun deploy(namespaceBytes: Bytes, namespaceSlot: NamespaceSlot)
    fun atSlot(namespaceSlot: NamespaceSlot): Optional<Namespace>
    fun all(): Stream<Optional<Namespace>>
    fun getCurrentNamespace(): Namespace
    fun updateCurrentNamespace(namespaceSlot: NamespaceSlot)
}
