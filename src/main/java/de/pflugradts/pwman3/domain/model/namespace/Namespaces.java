package de.pflugradts.pwman3.domain.model.namespace;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.CAPACITY;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.DEFAULT;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.FIRST;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.LAST;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"checkstyle:HideUtilityClassConstructor", "PMD.ClassNamingConventions"})
public class Namespaces {

    private static final List<Optional<Namespace>> NAMESPACES_LIST = new ArrayList<>();
    private static final NamespaceSlot CURRENT_NAMESPACE = DEFAULT;

    static void reset() {
        NAMESPACES_LIST.clear();
    }

    public static void populateEmpty() {
        populate(Collections.nCopies(CAPACITY, Bytes.empty()));
    }

    public static void populate(final List<Bytes> namespaceBytes) {
        if (NAMESPACES_LIST.isEmpty()) {
            if (namespaceBytes.size() == CAPACITY) {
                IntStream.range(FIRST, LAST + 1).forEach(index ->
                    NAMESPACES_LIST.add(namespaceBytes.get(index - 1).isEmpty()
                        ? Optional.empty()
                        : Optional.of(Namespace.create(namespaceBytes.get(index - 1), NamespaceSlot.at(index)))));
            } else {
                IntStream.range(FIRST, LAST + 1).forEach(x -> NAMESPACES_LIST.add(Optional.empty()));
            }
        }
    }

    private static List<Optional<Namespace>> getNamespaces() {
        if (NAMESPACES_LIST.isEmpty()) {
            populateEmpty();
        }
        return NAMESPACES_LIST;
    }

    public static Optional<Namespace> atSlot(final NamespaceSlot namespaceSlot) {
        return namespaceSlot == DEFAULT
            ? Optional.of(Namespace.DEFAULT)
            : getNamespaces().get(namespaceSlot.index() - 1);
    }

    public static Stream<Optional<Namespace>> all() {
        return getNamespaces().stream();
    }

    public static Namespace getCurrentNamespace() {
        return atSlot(CURRENT_NAMESPACE).orElse(Namespace.DEFAULT);
    }

}
