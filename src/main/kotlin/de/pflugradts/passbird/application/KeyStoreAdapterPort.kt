package de.pflugradts.passbird.application

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell
import java.nio.file.Path

interface KeyStoreAdapterPort {
    fun loadKey(password: PlainShell, path: Path): TryResult<Shell>
    fun storeKey(password: PlainShell, path: Path)
}
