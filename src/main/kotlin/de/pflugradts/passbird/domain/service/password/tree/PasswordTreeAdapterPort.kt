package de.pflugradts.passbird.domain.service.password.tree

interface PasswordTreeAdapterPort {
    fun restore(): EggStreamSupplier
    fun sync(supplier: EggStreamSupplier)
}
