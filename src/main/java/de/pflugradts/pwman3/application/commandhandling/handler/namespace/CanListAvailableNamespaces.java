package de.pflugradts.pwman3.application.commandhandling.handler.namespace;

import de.pflugradts.pwman3.domain.service.NamespaceService;

import java.util.Optional;
import java.util.stream.Collectors;

import static de.pflugradts.pwman3.domain.model.namespace.Namespace.DEFAULT;

public interface CanListAvailableNamespaces {

    default boolean hasCustomNamespaces(final NamespaceService namespaceService) {
        return namespaceService.all().anyMatch(Optional::isPresent);
    }

    default String getAvailableNamespaces(final NamespaceService namespaceService, final boolean includeCurrent) {
        final var defaultNamespaceLine = includeCurrent || !namespaceService.getCurrentNamespace().equals(DEFAULT)
            ? "\t0: " + DEFAULT.getBytes().asString() + System.lineSeparator() : "";
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
