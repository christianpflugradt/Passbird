package de.pflugradts.passbird.domain.service.password.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.passbird.domain.model.ddd.Repository;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.NamespaceService;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.pflugradts.passbird.domain.service.password.storage.PasswordEntryFilter.ALL_NAMESPACES;
import static de.pflugradts.passbird.domain.service.password.storage.PasswordEntryFilter.CURRENT_NAMESPACE;

@Singleton
public class PasswordEntryRepository implements Repository {

    @Inject
    private PasswordStoreAdapterPort passwordStoreAdapterPort;
    @Inject
    private NamespaceService namespaceService;
    @Inject
    private EventRegistry eventRegistry;

    private Set<PasswordEntry> passwordEntries;

    public void sync() {
        passwordStoreAdapterPort.sync(getPasswordEntriesSupplier(ALL_NAMESPACES));
    }

    public Optional<PasswordEntry> find(final Bytes keyBytes, final NamespaceSlot namespace) {
        return find(getPasswordEntriesSupplier(namespace), keyBytes);
    }

    public Optional<PasswordEntry> find(final Bytes keyBytes) {
        return find(getPasswordEntriesSupplier(CURRENT_NAMESPACE), keyBytes);
    }

    private Optional<PasswordEntry> find(final Supplier<Stream<PasswordEntry>> supplier, final Bytes keyBytes) {
        return supplier.get().filter(passwordEntry -> passwordEntry.viewKey().equals(keyBytes)).findAny();
    }

    public void add(final PasswordEntry passwordEntry) {
        eventRegistry.register(passwordEntry);
        getPasswordEntries().add(passwordEntry);
    }

    public void delete(final PasswordEntry passwordEntry) {
        getPasswordEntries().remove(passwordEntry);
        eventRegistry.deregister(passwordEntry);
    }

    public Stream<PasswordEntry> findAll() {
        return getPasswordEntriesSupplier(CURRENT_NAMESPACE).get();
    }

    private Set<PasswordEntry> getPasswordEntries() {
        if (Objects.isNull(passwordEntries)) {
            requestInitialization();
        }
        return passwordEntries;
    }

    private Supplier<Stream<PasswordEntry>> getPasswordEntriesSupplier(final PasswordEntryFilter passwordEntryFilter) {
        return getPasswordEntriesSupplier(passwordEntryFilter.equals(CURRENT_NAMESPACE)
            ? PasswordEntryFilter.Companion.inNamespace(namespaceService.getCurrentNamespace().getSlot())
            : PasswordEntryFilter.Companion.all());
    }

    private Supplier<Stream<PasswordEntry>> getPasswordEntriesSupplier(final NamespaceSlot namespace) {
        return getPasswordEntriesSupplier(PasswordEntryFilter.Companion.inNamespace(namespace));
    }

    private Supplier<Stream<PasswordEntry>> getPasswordEntriesSupplier(final Predicate<PasswordEntry> predicate) {
        return () -> getPasswordEntries().stream().filter(predicate);
    }

    public void requestInitialization() {
        passwordEntries = passwordStoreAdapterPort
            .restore()
            .get()
            .collect(Collectors.toSet());
        passwordEntries.forEach(passwordEntry -> {
            passwordEntry.clearDomainEvents();
            eventRegistry.register(passwordEntry);
        });
    }
}
