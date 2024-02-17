package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.passbird.domain.model.egg.Egg
import io.mockk.every

fun fakeEggRepository(instance: EggRepository, withEggs: List<Egg> = emptyList()) {
    every { instance.find(any()) } answers { optionOf(withEggs.find { it.viewEggId() == firstArg() }) }
    every { instance.find(any(), any()) } answers {
        optionOf(withEggs.find { it.viewEggId() == firstArg() && it.associatedNest() == secondArg() })
    }
    every { instance.findAll() } answers { withEggs.stream() }
    every { instance.sync() } returns Unit
}
