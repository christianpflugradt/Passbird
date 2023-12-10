package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.Optional
import java.util.stream.Stream

interface PasswordEntryRepository : Repository {
    fun sync()
    fun find(keyBytes: Bytes, nestSlot: Slot): Optional<PasswordEntry>
    fun find(keyBytes: Bytes): Optional<PasswordEntry>
    fun add(passwordEntry: PasswordEntry)
    fun delete(passwordEntry: PasswordEntry)
    fun findAll(): Stream<PasswordEntry>
}
