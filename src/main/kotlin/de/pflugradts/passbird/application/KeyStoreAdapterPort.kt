package de.pflugradts.passbird.application

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.application.security.Key
import de.pflugradts.passbird.domain.model.shell.PlainShell
import java.nio.file.Path

interface KeyStoreAdapterPort {
    fun loadKey(password: PlainShell, path: Path): TryResult<Key>
    fun storeKey(password: PlainShell, path: Path)
}
