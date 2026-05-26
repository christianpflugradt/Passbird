package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.FavoriteMap
import de.pflugradts.passbird.domain.model.shell.Shell
import io.mockk.every
import io.mockk.mockk

fun fakePasswordTreeAdapterPort(
    withEggs: List<Egg> = emptyList(),
    withFavorites: FavoriteMap = emptyFavorites(),
    withNests: List<Shell> = emptyList(),
    withSyncFailure: Exception? = null,
): PasswordTreeAdapterPort {
    val instance = mockk<PasswordTreeAdapterPort>()
    every { instance.restore() } returns EggStreamSupplier({ withEggs.stream() }, favorites = withFavorites, nests = withNests)
    every { instance.sync(any()) } answers {
        withSyncFailure?.let { failure(it) } ?: success(Unit)
    }
    return instance
}
