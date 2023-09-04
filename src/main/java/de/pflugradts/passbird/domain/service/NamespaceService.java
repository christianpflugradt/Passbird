package de.pflugradts.passbird.domain.service;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.namespace.Namespace;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.namespace.Namespaces;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.CAPACITY;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class NamespaceService {

    private boolean initialized;
    @Getter(AccessLevel.PROTECTED)
    private final Namespaces namespaces = new Namespaces();

    @Inject
    private PasswordEntryRepository passwordEntryRepository;

    public void populateEmpty() {
        populate(Collections.nCopies(CAPACITY, Bytes.emptyBytes()));
    }

    public void populate(final List<Bytes> namespaceBytes) {
        namespaces.populate(namespaceBytes);
        setInitialized();
    }

    public void deploy(final Bytes namespaceBytes, final NamespaceSlot namespaceSlot) {
        guard();
        namespaces.deploy(namespaceBytes, namespaceSlot);
        passwordEntryRepository.sync();
    }

    public Optional<Namespace> atSlot(final NamespaceSlot namespaceSlot) {
        guard();
        return namespaces.atSlot(namespaceSlot);
    }

    public Stream<Optional<Namespace>> all() {
        guard();
        return namespaces.all();
    }

    public Namespace getCurrentNamespace() {
        guard();
        return namespaces.getCurrentNamespace();
    }

    public void updateCurrentNamespace(final NamespaceSlot namespaceSlot) {
        guard();
        namespaces.updateCurrentNamespace(namespaceSlot);
    }

    private void guard() {
        if (!isInitialized()) {
            passwordEntryRepository.requestInitialization();
            populateEmpty();
        }
    }

    private boolean isInitialized() {
        return initialized;
    }

    private void setInitialized() {
        initialized = true;
    }

}
