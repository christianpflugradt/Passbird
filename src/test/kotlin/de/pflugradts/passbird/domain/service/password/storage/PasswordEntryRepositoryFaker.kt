package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.password.PasswordEntry
import io.mockk.every
import java.util.Optional

fun fakePasswordEntryRepository(
    instance: PasswordEntryRepository,
    withPasswordEntries: List<PasswordEntry> = emptyList(),
) {
    every { instance.find(any()) } answers { Optional.ofNullable(withPasswordEntries.find { it.viewKey() == firstArg() }) }
    every { instance.find(any(), any()) } answers {
        Optional.ofNullable(withPasswordEntries.find { it.viewKey() == firstArg() && it.associatedNamespace() == secondArg() })
    }
    every { instance.findAll() } answers { withPasswordEntries.stream() }
    every { instance.sync() } returns Unit
}
