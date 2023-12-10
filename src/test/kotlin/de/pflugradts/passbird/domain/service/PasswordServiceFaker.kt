package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.nest.Slot
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
    withNestService: NestService? = null,
) {
    every { instance.putPasswordEntry(any(), any()) } returns Unit
    every { instance.putPasswordEntries(any()) } returns Unit
    every { instance.findAllKeys() } answers {
        if (withNestService != null) {
            withPasswordEntries
                .filter { it.associatedNest() == withNestService.getCurrentNest().slot }
                .map { it.viewKey() }.stream()
        } else {
            withPasswordEntries.map { it.viewKey() }.stream()
        }
    }
    every { instance.viewPassword(any()) } answers {
        Optional.ofNullable(withPasswordEntries.find { it.viewKey() == firstArg() }?.viewPassword())
    }
    every { instance.entryExists(any(), any<PasswordService.EntryNotExistsAction>()) } answers {
        withPasswordEntries.find { it.viewKey() == firstArg() } != null
    }
    every { instance.entryExists(any(), any<Slot>()) } answers {
        val res = withPasswordEntries.find { it.viewKey() == firstArg() && it.associatedNest() == secondArg() } != null
        println(res)
        res
    }
    if (withInvalidAlias) {
        every { instance.challengeAlias(any()) } throws InvalidKeyException(emptyBytes())
    } else {
        every { instance.challengeAlias(any()) } returns Unit
    }
    every { instance.discardPasswordEntry(any()) } returns Unit
    every { instance.renamePasswordEntry(any(), any()) } returns Unit
    every { instance.movePasswordEntry(any(), any()) } returns Unit
}
