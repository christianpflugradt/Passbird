package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.egg.Egg
import io.mockk.every
import io.mockk.mockk
import java.util.function.Supplier

fun fakePasswordStoreAdapterPort(withEggs: List<Egg> = emptyList()): PasswordStoreAdapterPort {
    val instance = mockk<PasswordStoreAdapterPort>()
    every { instance.restore() } returns Supplier { withEggs.stream() }
    every { instance.sync(any()) } returns Unit
    return instance
}
