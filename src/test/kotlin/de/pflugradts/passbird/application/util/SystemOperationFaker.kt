package de.pflugradts.passbird.application.util

import io.mockk.every
import io.mockk.mockk
import java.nio.file.Path
import java.security.KeyStoreException

fun fakeSystemOperation(
    instance: SystemOperation,
    withConsoleEnabled: Boolean = true,
    withPasswordFromConsole: CharArray = CharArray(0),
    withKeyStoreUnavailable: Boolean = false,
    withPaths: List<Pair<String, Path>> = emptyList(),
) {
    every { instance.newInputStream(any()) } returns mockk()
    every { instance.newOutputStream(any()) } returns mockk()
    every { instance.isConsoleAvailable } returns withConsoleEnabled
    every { instance.readPasswordFromConsole() } returns withPasswordFromConsole
    if (withKeyStoreUnavailable) every { instance.jceksInstance } throws KeyStoreException()
    withPaths.forEach { every { instance.getPath(it.first) } returns it.second }
    every { instance.exit() } returns Unit
}
