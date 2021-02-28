package de.pflugradts.pwman3.domain.service.password.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.domain.model.ddd.Repository;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.NamespaceService;
import de.pflugradts.pwman3.domain.service.eventhandling.EventRegistry;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryFilter.ALL_NAMESPACES;
import static de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryFilter.CURRENT_NAMESPACE;

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
            initializeRepository();
        }
        return passwordEntries;
    }

    private Supplier<Stream<PasswordEntry>> getPasswordEntriesSupplier(final PasswordEntryFilter passwordEntryFilter) {
        return getPasswordEntriesSupplier(passwordEntryFilter.equals(CURRENT_NAMESPACE)
            ? PasswordEntryFilter.inNamespace(namespaceService.getCurrentNamespace().getSlot())
            : PasswordEntryFilter.all());
    }

    private Supplier<Stream<PasswordEntry>> getPasswordEntriesSupplier(final NamespaceSlot namespace) {
        return getPasswordEntriesSupplier(PasswordEntryFilter.inNamespace(namespace));
    }

    private Supplier<Stream<PasswordEntry>> getPasswordEntriesSupplier(final Predicate<PasswordEntry> predicate) {
        return () -> getPasswordEntries().stream().filter(predicate);
    }

    public void requestInitialization() {
        initializeRepository();
    }

    private void initializeRepository() {
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
