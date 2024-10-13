package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every

fun fakeEggRepository(instance: EggRepository, withEggs: List<Egg> = emptyList()) {
    every { instance.findAll() } answers { withEggs.stream() }
    every { instance.findAll(any<Slot>()) } answers { withEggs.filter { it.associatedNest() == firstArg() }.stream() }
    every { instance.sync() } returns Unit
}
