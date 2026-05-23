package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every

fun fakeEggRepository(instance: EggRepository, withEggs: List<Egg> = emptyList(), withSyncFailure: Exception? = null) {
    every { instance.findAll() } answers { withEggs.stream() }
    every { instance.findAll(any<Slot>()) } answers { withEggs.filter { it.associatedNest() == firstArg() }.stream() }
    every { instance.memory() } returns EggIdMemory()
    every { instance.updateMemory(any()) } returns Unit
    every { instance.sync() } answers {
        withSyncFailure?.let { failure(it) } ?: success(Unit)
    }
}
