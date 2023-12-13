package de.pflugradts.passbird.adapter.passwordstore

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.service.password.storage.EggStreamSupplier
import de.pflugradts.passbird.domain.service.password.storage.PasswordStoreAdapterPort

@Singleton
class PasswordStoreFacade @Inject constructor(
    @Inject val passwordStoreReader: PasswordStoreReader,
    @Inject val passwordStoreWriter: PasswordStoreWriter,
) : PasswordStoreAdapterPort {
    override fun restore(): EggStreamSupplier = passwordStoreReader.restore()
    override fun sync(supplier: EggStreamSupplier) { passwordStoreWriter.sync(supplier) }
}
