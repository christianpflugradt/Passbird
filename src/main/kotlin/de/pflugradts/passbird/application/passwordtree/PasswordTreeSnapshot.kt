package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.FavoriteMap
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.tree.emptyFavorites
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory

data class PasswordTreeSnapshot(
    val eggs: List<Egg> = emptyList(),
    val favorites: FavoriteMap = emptyFavorites(),
    val memory: MemoryMap = emptyMemory(),
    val nests: List<Shell> = List(Slot.CAPACITY) { emptyShell() },
) {
    init {
        require(nests.size == Slot.CAPACITY)
    }
}
