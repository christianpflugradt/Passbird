package de.pflugradts.passbird.application.util

import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.FileName
import de.pflugradts.passbird.application.toDirectory
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.nio.file.Path
import java.security.KeyStoreException
import java.time.Clock

fun fakeSystemOperation(
    instance: SystemOperation,
    withClock: Clock = Clock.systemUTC(),
    withConsoleEnabled: Boolean = true,
    withPasswordFromConsole: CharArray = CharArray(0),
    withKeyStoreUnavailable: Boolean = false,
    withPaths: List<Pair<String, Path>> = emptyList(),
    withDirectoryResolvingToFileName: Triple<Directory, FileName, Path>? = null,
    withIoException: Boolean = false,
) {
    every { instance.clock } returns withClock
    every { instance.newInputStream(any()) } returns mockk()
    every { instance.newOutputStream(any()) } returns mockk()
    every { instance.isConsoleAvailable } returns withConsoleEnabled
    every { instance.readPasswordFromConsole() } returns withPasswordFromConsole
    if (withKeyStoreUnavailable) every { instance.jceksInstance } throws KeyStoreException()
    withPaths.forEach { every { instance.getPath(it.first.toDirectory()) } returns it.second }
    withDirectoryResolvingToFileName?.run { every { instance.resolvePath(first, second) } returns third }
    every { instance.exit() } returns Unit
    if (withIoException) { every { instance.resolvePath(any(), any()) } throws IOException() }
}
