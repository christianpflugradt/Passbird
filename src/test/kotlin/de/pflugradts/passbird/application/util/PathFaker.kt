package de.pflugradts.passbird.application.util

import io.mockk.every
import io.mockk.mockk
import java.nio.file.Path

fun fakePath(
    instance: Path = mockk(relaxed = true),
    resolvingTo: Pair<Path, String>? = null,
    exists: Boolean = false,
    isDirectory: Boolean = false,
    withParent: Path? = null,
): Path {
    every { instance.toFile().exists() } returns exists
    every { instance.toFile().isDirectory() } returns isDirectory
    resolvingTo?.let { every { instance.resolve(it.second) } returns it.first }
    every { instance.toFile().parentFile } returns withParent?.toFile()
    return instance
}
