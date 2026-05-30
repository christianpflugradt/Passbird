package de.pflugradts.passbird.application.util

import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.FileName
import de.pflugradts.passbird.application.toDirectory
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock

fun fakeSystemOperation(
    instance: SystemOperation,
    withClock: Clock = Clock.systemUTC(),
    withPaths: List<Pair<String, Path>> = emptyList(),
    withDirectoryResolvingToFileName: Triple<Directory, FileName, Path>? = null,
    withIoException: Boolean = false,
) {
    every { instance.clock } returns withClock
    every { instance.newInputStream(any()) } returns mockk()
    every { instance.newOutputStream(any()) } returns mockk()
    every { instance.writeToSensitiveFile(any(), any()) } answers {
        secondArg<(OutputStream) -> Unit>().invoke(mockk<OutputStream>(relaxed = true))
        Paths.get("")
    }
    withPaths.forEach { every { instance.getPath(it.first.toDirectory()) } returns it.second }
    withDirectoryResolvingToFileName?.run { every { instance.resolvePath(first, second) } returns third }
    every { instance.exit(any()) } returns Unit
    if (withIoException) every { instance.resolvePath(any(Directory::class), any(FileName::class)) } throws IOException()
    if (withIoException) every { instance.resolvePath(any(Directory::class), any(Directory::class)) } throws IOException()
    if (withIoException) every { instance.writeToSensitiveFile(any(), any()) } throws IOException()
}
