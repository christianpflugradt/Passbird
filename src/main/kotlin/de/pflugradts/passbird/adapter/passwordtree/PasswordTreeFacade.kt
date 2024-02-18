package de.pflugradts.passbird.adapter.passwordtree

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort

@Singleton
class PasswordTreeFacade @Inject constructor(
    @Inject val passwordTreeReader: PasswordTreeReader,
    @Inject val passwordTreeWriter: PasswordTreeWriter,
) : PasswordTreeAdapterPort {
    override fun restore(): EggStreamSupplier = passwordTreeReader.restore()
    override fun sync(supplier: EggStreamSupplier) = passwordTreeWriter.sync(supplier)
}
