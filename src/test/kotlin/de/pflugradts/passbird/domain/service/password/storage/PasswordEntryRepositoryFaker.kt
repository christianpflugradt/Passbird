package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.egg.Egg
import io.mockk.every
import java.util.Optional

fun fakeEggRepository(
    instance: EggRepository,
    withEggs: List<Egg> = emptyList(),
) {
    every { instance.find(any()) } answers { Optional.ofNullable(withEggs.find { it.viewKey() == firstArg() }) }
    every { instance.find(any(), any()) } answers {
        Optional.ofNullable(withEggs.find { it.viewKey() == firstArg() && it.associatedNest() == secondArg() })
    }
    every { instance.findAll() } answers { withEggs.stream() }
    every { instance.sync() } returns Unit
}
