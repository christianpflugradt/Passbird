package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.egg.Egg
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * AdapterPort for syncing the Password Repository with and restoring it from a physical file.
 */
interface PasswordStoreAdapterPort {
    fun restore(): Supplier<Stream<Egg>>
    fun sync(eggs: Supplier<Stream<Egg>>)
}
