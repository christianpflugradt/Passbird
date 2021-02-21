package de.pflugradts.pwman3.domain.service.password.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.domain.model.ddd.Repository;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.NamespaceService;
import de.pflugradts.pwman3.domain.service.eventhandling.EventRegistry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        passwordStoreAdapterPort.sync(getPasswordEntriesSupplier());
    }

    public Optional<PasswordEntry> find(final Bytes keyBytes) {
        return getPasswordEntriesSupplier()
                .get()
                .filter(passwordEntry -> passwordEntry.viewKey().equals(keyBytes))
                .findAny();
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
        return getPasswordEntriesSupplier().get();
    }

    private Set<PasswordEntry> getPasswordEntries() {
        if (Objects.isNull(passwordEntries)) {
            initializeRepository();
        }
        return passwordEntries;
    }

    private boolean inCurrentNamespace(final PasswordEntry passwordEntry) {
        return passwordEntry.associatedNamespace().equals(namespaceService.getCurrentNamespace().getSlot());
    }

    private Supplier<Stream<PasswordEntry>> getPasswordEntriesSupplier() {
        return () -> getPasswordEntries().stream().filter(this::inCurrentNamespace);
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
