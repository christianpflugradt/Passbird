package de.pflugradts.passbird.application

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.application.security.Key
import de.pflugradts.passbird.domain.model.transfer.Chars
import java.nio.file.Path

interface KeyStoreAdapterPort {
    fun loadKey(password: Chars, path: Path): TryResult<Key>
    fun storeKey(password: Chars, path: Path)
}
