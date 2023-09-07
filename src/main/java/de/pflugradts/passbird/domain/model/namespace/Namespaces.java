package de.pflugradts.passbird.domain.model.namespace;

import de.pflugradts.passbird.domain.model.transfer.Bytes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.CAPACITY;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.FIRST_NAMESPACE;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.LAST_NAMESPACE;

public class Namespaces {

    private final List<Optional<Namespace>> namespacesList = new ArrayList<>();
    private NamespaceSlot currentNamespace = DEFAULT;

    public void reset() {
        namespacesList.clear();
    }

    private List<Optional<Namespace>> getNamespaces() {
        return namespacesList;
    }

    public void populate(final List<Bytes> namespaceBytes) {
        if (getNamespaces().isEmpty()) {
            if (namespaceBytes.size() == CAPACITY) {
                IntStream.range(FIRST_NAMESPACE, LAST_NAMESPACE + 1).forEach(index ->
                    getNamespaces().add(namespaceBytes.get(index - 1).isEmpty()
                        ? Optional.empty()
                        : Optional.of(Namespace.create(namespaceBytes.get(index - 1), NamespaceSlot.at(index)))));
            } else {
                IntStream.range(FIRST_NAMESPACE, LAST_NAMESPACE + 1).forEach(x -> getNamespaces().add(Optional.empty()));
            }
        }
    }

    public void deploy(final Bytes namespaceBytes, final NamespaceSlot namespaceSlot) {
        getNamespaces().set(namespaceSlot.index() - 1, Optional.of(Namespace.create(namespaceBytes, namespaceSlot)));
    }

    public Optional<Namespace> atSlot(final NamespaceSlot namespaceSlot) {
        return namespaceSlot == DEFAULT
            ? Optional.of(Namespace.DEFAULT)
            : getNamespaces().get(namespaceSlot.index() - 1);
    }

    public Stream<Optional<Namespace>> all() {
        return getNamespaces().stream();
    }

    public Namespace getCurrentNamespace() {
        return atSlot(currentNamespace).orElse(Namespace.DEFAULT);
    }

    public void updateCurrentNamespace(final NamespaceSlot namespaceSlot) {
        if (atSlot(namespaceSlot).isPresent()) {
            currentNamespace = namespaceSlot;
        }
    }

}
