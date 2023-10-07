package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import java.util.Optional

fun fakePasswordService(
    instance: PasswordService,
    withPasswordEntries: List<PasswordEntry> = emptyList(),
) {
    every { instance.putPasswordEntries(any()) } returns Unit
    every { instance.findAllKeys() } returns withPasswordEntries.map { it.viewKey() }.stream()
    every { instance.viewPassword(any()) } answers {
        Optional.ofNullable(withPasswordEntries.find { it.viewKey() == firstArg() }?.viewPassword())
    }
}
