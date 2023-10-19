package de.pflugradts.passbird.domain.service.password.storage

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.NamespaceService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryFilter.Companion.all
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryFilter.Companion.inNamespace
import java.util.Optional
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream

@Singleton
class NamespaceBasedPasswordEntryRepository @Inject constructor(
    @Inject private val passwordStoreAdapterPort: PasswordStoreAdapterPort,
    @Inject private val namespaceService: NamespaceService,
    @Inject private val eventRegistry: EventRegistry,
) : PasswordEntryRepository {

    private val passwordEntries = passwordStoreAdapterPort.restore().get().toList().toMutableList()

    init {
        passwordEntries.forEach {
            it.clearDomainEvents()
            eventRegistry.register(it)
        }
    }

    override fun sync() { passwordStoreAdapterPort.sync(getPasswordEntriesSupplier(PasswordEntryFilter.ALL_NAMESPACES)) }
    override fun find(keyBytes: Bytes, namespace: NamespaceSlot): Optional<PasswordEntry> =
        find(getPasswordEntriesSupplier(namespace), keyBytes)
    override fun find(keyBytes: Bytes): Optional<PasswordEntry> =
        find(getPasswordEntriesSupplier(PasswordEntryFilter.CURRENT_NAMESPACE), keyBytes)

    private fun find(supplier: Supplier<Stream<PasswordEntry>>, keyBytes: Bytes): Optional<PasswordEntry> =
        supplier.get().filter { it.viewKey() == keyBytes }.findAny()

    override fun add(passwordEntry: PasswordEntry) {
        eventRegistry.register(passwordEntry)
        passwordEntries.add(passwordEntry)
    }

    override fun delete(passwordEntry: PasswordEntry) {
        passwordEntries.remove(passwordEntry)
        eventRegistry.deregister(passwordEntry)
    }

    override fun findAll() = getPasswordEntriesSupplier(PasswordEntryFilter.CURRENT_NAMESPACE).get()

    private fun getPasswordEntriesSupplier(passwordEntryFilter: PasswordEntryFilter): Supplier<Stream<PasswordEntry>> =
        getPasswordEntriesSupplier(
            if ((passwordEntryFilter == PasswordEntryFilter.CURRENT_NAMESPACE)) {
                inNamespace(namespaceService.getCurrentNamespace().slot)
            } else {
                all()
            },
        )

    private fun getPasswordEntriesSupplier(namespace: NamespaceSlot) = getPasswordEntriesSupplier(inNamespace(namespace))

    private fun getPasswordEntriesSupplier(predicate: Predicate<PasswordEntry>) = Supplier { passwordEntries.stream().filter(predicate) }
}
