package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.password.PasswordEntry
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * AdapterPort for syncing the Password Repository with and restoring it from a physical file.
 */
interface PasswordStoreAdapterPort {
    fun restore(): Supplier<Stream<PasswordEntry>>
    fun sync(passwordEntries: Supplier<Stream<PasswordEntry>>)
}
