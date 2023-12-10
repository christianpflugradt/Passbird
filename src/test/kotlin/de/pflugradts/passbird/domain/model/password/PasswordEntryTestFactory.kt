package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.nest.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.password.PasswordEntry.Companion.createPasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf

fun createPasswordEntryForTesting(
    withKeyBytes: Bytes = bytesOf("key"),
    withPasswordBytes: Bytes = bytesOf("password"),
    withNestSlot: Slot = DEFAULT,
): PasswordEntry = createPasswordEntry(withNestSlot, withKeyBytes, withPasswordBytes)
