package de.pflugradts.passbird.application.commandhandling.handler.namespace;

import de.pflugradts.passbird.domain.model.namespace.Namespace;
import de.pflugradts.passbird.domain.service.FixedNamespaceService;

import java.util.Optional;
import java.util.stream.Collectors;

public interface CanListAvailableNamespaces {

    default boolean hasCustomNamespaces(final FixedNamespaceService namespaceService) {
        return namespaceService.all().anyMatch(Optional::isPresent);
    }

    default String getAvailableNamespaces(final FixedNamespaceService namespaceService, final boolean includeCurrent) {
        final var defaultNamespaceLine = includeCurrent || !namespaceService.getCurrentNamespace().equals(Namespace.Companion.getDEFAULT())
            ? "\t0: " + Namespace.Companion.getDEFAULT().getBytes().asString() + System.lineSeparator() : "";
        return defaultNamespaceLine + namespaceService.all()
            .filter(Optional::isPresent)
            .filter(namespace -> includeCurrent || !namespace.get().equals(namespaceService.getCurrentNamespace()))
            .map(Optional::get)
            .map(namespace -> "\t"
                + namespace.getSlot().index()
                + ": "
                + namespace.getBytes().asString()
                + System.lineSeparator())
            .collect(Collectors.joining());
    }

}
