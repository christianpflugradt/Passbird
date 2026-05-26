package de.pflugradts.passbird.adapter.passwordtree

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class PasswordTreeFacade @Inject constructor(
    val passwordTreeReader: PasswordTreeReader,
    val passwordTreeWriter: PasswordTreeWriter,
) : PasswordTreeAdapterPort {
    override fun restore(): EggStreamSupplier = passwordTreeReader.restore()
    override fun sync(snapshot: EggStreamSupplier): TryResult<Unit> = passwordTreeWriter.sync(snapshot)
}
