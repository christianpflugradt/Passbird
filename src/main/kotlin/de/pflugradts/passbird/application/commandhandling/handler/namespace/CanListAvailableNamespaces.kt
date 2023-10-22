package de.pflugradts.passbird.application.commandhandling.handler.namespace

import de.pflugradts.passbird.domain.model.namespace.Namespace.Companion.DEFAULT
import de.pflugradts.passbird.domain.service.NamespaceService

abstract class CanListAvailableNamespaces(
    private val namespaceService: NamespaceService,
) {
    fun hasCustomNamespaces() = namespaceService.all().anyMatch { it.isPresent }
    fun getAvailableNamespaces(includeCurrent: Boolean) =
        (if (includeCurrent || namespaceService.getCurrentNamespace() != DEFAULT) "\t0: ${DEFAULT.bytes.asString()}\n" else "") +
            namespaceService.all()
                .filter { it.isPresent }
                .map { it.get() }
                .filter { includeCurrent || it != namespaceService.getCurrentNamespace() }
                .map { "\t${it.slot.index()}: ${it.bytes.asString()}" }
                .toList().joinToString("\n")
}
