package de.pflugradts.passbird.application.util

import io.mockk.every
import io.mockk.mockk
import java.nio.file.Path

fun fakePath(
    instance: Path = mockk<Path>(),
    resolvingTo: Pair<Path, String>? = null,
): Path {
    resolvingTo?.let { every { instance.resolve(it.second) } returns it.first }
    return instance
}
