package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.password.InvalidKeyException
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import java.util.Optional

fun fakePasswordService(
    instance: PasswordService,
    withInvalidAlias: Boolean = false,
    withPasswordEntries: List<PasswordEntry> = emptyList(),
) {
    every { instance.putPasswordEntry(any(), any()) } returns Unit
    every { instance.putPasswordEntries(any()) } returns Unit
    every { instance.findAllKeys() } returns withPasswordEntries.map { it.viewKey() }.stream()
    every { instance.viewPassword(any()) } answers {
        Optional.ofNullable(withPasswordEntries.find { it.viewKey() == firstArg() }?.viewPassword())
    }
    every { instance.entryExists(any(), any<PasswordService.EntryNotExistsAction>()) } answers {
        withPasswordEntries.find { it.viewKey() == firstArg() } != null
    }
    if (withInvalidAlias) {
        every { instance.challengeAlias(any()) } throws InvalidKeyException(emptyBytes())
    } else {
        every { instance.challengeAlias(any()) } returns Unit
    }
    every { instance.discardPasswordEntry(any()) } returns Unit
}
