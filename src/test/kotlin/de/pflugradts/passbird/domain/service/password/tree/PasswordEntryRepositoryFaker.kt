package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.fakeDec
import io.mockk.every

fun fakeEggRepository(instance: EggRepository, withEggs: List<Egg> = emptyList()) {
    every { instance.find(any<Shell>()) } answers { optionOf(withEggs.find { it.viewEggId().fakeDec() == firstArg() }) }
    every { instance.find(any<Shell>(), any()) } answers {
        optionOf(withEggs.find { it.viewEggId().fakeDec() == firstArg() && it.associatedNest() == secondArg() })
    }
    every { instance.findAll() } answers { withEggs.stream() }
    every { instance.sync() } returns Unit
}
