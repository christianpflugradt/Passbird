package de.pflugradts.passbird.domain.model.event

import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes

data class PasswordEntryCreated(val passwordEntry: PasswordEntry) : DomainEvent
data class PasswordEntryDiscarded(val passwordEntry: PasswordEntry) : DomainEvent
data class PasswordEntryNotFound(val keyBytes: Bytes) : DomainEvent
data class PasswordEntryRenamed(val passwordEntry: PasswordEntry) : DomainEvent
data class PasswordEntryUpdated(val passwordEntry: PasswordEntry) : DomainEvent
