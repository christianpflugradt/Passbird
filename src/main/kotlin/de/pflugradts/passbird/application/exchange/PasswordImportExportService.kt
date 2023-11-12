package de.pflugradts.passbird.application.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.NamespaceService
import de.pflugradts.passbird.domain.service.password.PasswordService

class PasswordImportExportService @Inject constructor(
    @Inject private val exchangeFactory: ExchangeFactory,
    @Inject private val passwordService: PasswordService,
    @Inject private val namespaceService: NamespaceService,
) : ImportExportService {
    override fun peekImportKeyBytes(uri: String) = exchangeFactory.createPasswordExchange(uri).receive().entries.associate {
        it.key to it.value.map { bytePair -> bytePair.value.first }
    }

    override fun importPasswordEntries(uri: String) {
        val currentNamespace = namespaceService.getCurrentNamespace()
        val passwordEntriesByNamespaces = exchangeFactory.createPasswordExchange(uri).receive()
        passwordEntriesByNamespaces.keys.forEach { slot ->
            val deployedNamespace = namespaceService.atSlot(slot)
            if (deployedNamespace.isEmpty) {
                namespaceService.deploy(bytesOf("Namespace-${slot.index()}"), slot)
            }
            namespaceService.updateCurrentNamespace(slot)
            passwordService.putPasswordEntries(passwordEntriesByNamespaces[slot]!!.stream())
        }
        namespaceService.updateCurrentNamespace(currentNamespace.slot)
    }

    override fun exportPasswordEntries(uri: String) {
        val currentNamespace = namespaceService.getCurrentNamespace()
        val passwordEntriesByNamespaces = mutableMapOf<NamespaceSlot, List<BytePair>>()
        namespaceService.all(includeDefault = true).filter { it.isPresent }.map { it.get() }.forEach { namespace ->
            namespaceService.updateCurrentNamespace(namespace.slot)
            passwordEntriesByNamespaces[namespace.slot] = passwordService.findAllKeys()
                .map { key -> BytePair(Pair(key, passwordService.viewPassword(key).get())) }.toList()
        }
        exchangeFactory.createPasswordExchange(uri).send(passwordEntriesByNamespaces)
        namespaceService.updateCurrentNamespace(currentNamespace.slot)
    }
}
