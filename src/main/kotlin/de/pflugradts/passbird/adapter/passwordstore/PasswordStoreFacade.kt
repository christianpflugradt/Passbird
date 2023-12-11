package de.pflugradts.passbird.adapter.passwordstore

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.service.password.storage.PasswordStoreAdapterPort
import java.util.function.Supplier
import java.util.stream.Stream

@Singleton
class PasswordStoreFacade @Inject constructor(
    @Inject val passwordStoreReader: PasswordStoreReader,
    @Inject val passwordStoreWriter: PasswordStoreWriter,
) : PasswordStoreAdapterPort {
    override fun restore(): Supplier<Stream<Egg>> = passwordStoreReader.restore()
    override fun sync(eggs: Supplier<Stream<Egg>>) { passwordStoreWriter.sync(eggs) }
}
