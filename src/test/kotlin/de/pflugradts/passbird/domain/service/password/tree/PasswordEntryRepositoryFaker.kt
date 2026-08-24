package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every

fun fakeEggRepository(instance: EggRepository, withEggs: List<Egg> = emptyList(), withSyncFailure: Exception? = null) {
    every { instance.findAll() } answers { withEggs.stream() }
    every { instance.findAll(any<Slot>()) } answers { withEggs.filter { it.associatedNest() == firstArg() }.stream() }
    every { instance.favorites() } returns EggIdFavorites()
    every { instance.memory() } returns EggIdMemory()
    every { instance.putFavorite(any(), any()) } returns Unit
    every { instance.discardFavorite(any()) } returns Unit
    every { instance.discardFavorites(any<Slot>(), any()) } returns Unit
    every { instance.discardFavorites(any<Slot>()) } returns Unit
    every { instance.discardMemory(any<Slot>(), any()) } returns Unit
    every { instance.renameFavorites(any(), any(), any()) } returns Unit
    every { instance.updateMemory(any(), any(), any()) } returns Unit
    every { instance.sync() } answers {
        withSyncFailure?.let { failure(it) } ?: success(Unit)
    }
}
