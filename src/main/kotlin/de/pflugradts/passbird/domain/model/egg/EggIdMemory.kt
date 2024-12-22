package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slots

typealias MemoryMap = Slots<EggIdMemory>

class EggIdMemory : Slots<EncryptedShell>() {
    fun memorize(encryptedShell: EncryptedShell, duplicate: EncryptedShell?) {
        val startIndex = this.indexOfFirst { it.map { item -> item == duplicate }.orElse(false) }
            .takeIf { it != -1 } ?: this.indexOfFirst { it.isEmpty }.takeIf { it != -1 } ?: Slot.S9.index()
        (startIndex downTo 1).forEach { this[it].set(this[it - 1].get()) }
        this[0].set(encryptedShell)
    }

    fun copy() = EggIdMemory().apply { this@EggIdMemory.forEachIndexed { index, item -> this[index] = item.map(EncryptedShell::copy) } }
}
