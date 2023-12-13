package de.pflugradts.passbird.domain.service.password.storage

/**
 * AdapterPort for syncing the Password Repository with and restoring it from a physical file.
 */
interface PasswordStoreAdapterPort {
    fun restore(): EggStreamSupplier
    fun sync(supplier: EggStreamSupplier)
}
