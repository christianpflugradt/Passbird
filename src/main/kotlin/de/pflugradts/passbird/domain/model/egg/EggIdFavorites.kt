package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slots

typealias FavoriteMap = Slots<EggIdFavorites>

class EggIdFavorites : Slots<EncryptedShell>() {
    fun assign(slot: Slot, encryptedShell: EncryptedShell) {
        this[slot].set(encryptedShell.copy())
    }

    fun discard(slot: Slot) {
        this[slot].set(null)
    }

    fun discard(encryptedShell: EncryptedShell) {
        forEach { favorite ->
            if (favorite.map { it == encryptedShell }.orElse(false)) {
                favorite.set(null)
            }
        }
    }

    fun rename(from: EncryptedShell, to: EncryptedShell) {
        forEach { favorite ->
            if (favorite.map { it == from }.orElse(false)) {
                favorite.set(to.copy())
            }
        }
    }

    fun copy() = EggIdFavorites().apply {
        this@EggIdFavorites.forEachIndexed { index, item -> this[index] = item.map(EncryptedShell::copy) }
    }
}
