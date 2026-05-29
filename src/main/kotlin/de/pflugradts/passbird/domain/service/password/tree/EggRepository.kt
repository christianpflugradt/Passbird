package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.slot.Slot
import java.util.stream.Stream

interface EggRepository : Repository {
    fun sync(): TryResult<Unit>
    fun add(egg: Egg)
    fun delete(egg: Egg)
    fun findAll(slot: Slot): Stream<Egg>
    fun findAll(): Stream<Egg>
    fun favorites(): EggIdFavorites
    fun memory(): EggIdMemory
    fun putFavorite(slot: Slot, encryptedShell: EncryptedShell)
    fun discardFavorite(slot: Slot)
    fun discardFavorites(nestSlot: Slot, encryptedShell: EncryptedShell)
    fun discardFavorites(nestSlot: Slot)
    fun renameFavorites(nestSlot: Slot, from: EncryptedShell, to: EncryptedShell)
    fun updateMemory(mostRecentEgg: Egg, duplicate: EncryptedShell? = null, sync: Boolean = true)
}
