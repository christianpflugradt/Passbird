package de.pflugradts.passbird.domain.service

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.namespace.Namespace
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import java.util.Collections
import java.util.Optional

class FixedNamespaceService @Inject constructor(
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
) : NamespaceService {

    private val namespaces = Collections.nCopies(NamespaceSlot.CAPACITY, Optional.empty<Namespace>()).toMutableList()
    private var currentNamespace = NamespaceSlot.DEFAULT

    override fun populate(namespaceBytes: List<Bytes>) {
        if (namespaceBytes.size == NamespaceSlot.CAPACITY) {
            (NamespaceSlot.FIRST_NAMESPACE..NamespaceSlot.LAST_NAMESPACE).forEach {
                if (namespaceBytes[it - 1].isNotEmpty) {
                    namespaces[it - 1] = Optional.of(Namespace.createNamespace(namespaceBytes[it - 1], NamespaceSlot.at(it)))
                }
            }
        }
    }

    override fun deploy(namespaceBytes: Bytes, namespaceSlot: NamespaceSlot) {
        namespaces[namespaceSlot.index() - 1] = Optional.of(Namespace.createNamespace(namespaceBytes, namespaceSlot))
        passwordEntryRepository.sync()
    }
    override fun atSlot(namespaceSlot: NamespaceSlot): Optional<Namespace> =
        if (namespaceSlot === NamespaceSlot.DEFAULT) Optional.of(Namespace.DEFAULT) else namespaces[namespaceSlot.index() - 1]
    override fun all() = namespaces.stream()
    override fun getCurrentNamespace(): Namespace = atSlot(currentNamespace).orElse(Namespace.DEFAULT)
    override fun updateCurrentNamespace(namespaceSlot: NamespaceSlot) {
        if (atSlot(namespaceSlot).isPresent) { currentNamespace = namespaceSlot }
    }
}
