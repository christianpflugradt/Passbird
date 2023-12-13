package de.pflugradts.passbird.domain.service.password.storage

interface PasswordStoreAdapterPort {
    fun restore(): EggStreamSupplier
    fun sync(supplier: EggStreamSupplier)
}
