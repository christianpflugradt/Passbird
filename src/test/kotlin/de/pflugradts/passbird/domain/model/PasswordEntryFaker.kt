package de.pflugradts.passbird.domain.model

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf

fun fakePasswordEntry(
    withKeyBytes: Bytes = bytesOf("key"),
    withPasswordBytes: Bytes = bytesOf("password"),
    withNamespace: NamespaceSlot = DEFAULT,
): PasswordEntry = PasswordEntry.create(withNamespace, withKeyBytes, withPasswordBytes)
