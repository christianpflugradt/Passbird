package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.domain.model.egg.Egg
import io.mockk.every
import io.mockk.mockk

fun fakePasswordTreeAdapterPort(withEggs: List<Egg> = emptyList()): PasswordTreeAdapterPort {
    val instance = mockk<PasswordTreeAdapterPort>()
    every { instance.restore() } returns EggStreamSupplier({ withEggs.stream() })
    every { instance.sync(any()) } returns Unit
    return instance
}
