package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.TryResult

interface PasswordTreeAdapterPort {
    fun restore(): EggStreamSupplier
    fun sync(snapshot: EggStreamSupplier): TryResult<Unit>
}
