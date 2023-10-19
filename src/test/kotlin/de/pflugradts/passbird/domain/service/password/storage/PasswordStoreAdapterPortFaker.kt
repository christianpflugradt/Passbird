package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.password.PasswordEntry
import io.mockk.every
import io.mockk.mockk
import java.util.function.Supplier

fun fakePasswordStoreAdapterPort(
    withPasswordEntries: List<PasswordEntry> = emptyList(),
): PasswordStoreAdapterPort {
    val instance = mockk<PasswordStoreAdapterPort>()
    every { instance.restore() } returns Supplier { withPasswordEntries.stream() }
    every { instance.sync(any()) } returns Unit
    return instance
}
