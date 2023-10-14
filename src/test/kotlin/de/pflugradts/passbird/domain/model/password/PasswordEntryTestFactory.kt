package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT
import de.pflugradts.passbird.domain.model.password.PasswordEntry.Companion.createPasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf

fun createPasswordEntryForTesting(
    withKeyBytes: Bytes = bytesOf("key"),
    withPasswordBytes: Bytes = bytesOf("password"),
    withNamespace: NamespaceSlot = DEFAULT,
): PasswordEntry = createPasswordEntry(withNamespace, withKeyBytes, withPasswordBytes)
