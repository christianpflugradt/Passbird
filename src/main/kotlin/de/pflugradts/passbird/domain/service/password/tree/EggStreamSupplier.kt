package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.FavoriteMap
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slots
import java.util.function.Supplier
import java.util.stream.Stream

class EggStreamSupplier(
    private val delegate: Supplier<Stream<Egg>>,
    private val memory: MemoryMap = emptyMemory(),
    private val favorites: FavoriteMap = emptyFavorites(),
    private val nests: List<Shell> = List(Slot.CAPACITY) { emptyShell() },
) : Supplier<Stream<Egg>> by delegate {
    fun favorites(): FavoriteMap = favorites.copyUsing { it.copy() }
    fun memory(): MemoryMap = memory.copyUsing { it.copy() }
    fun nests(): List<Shell> = nests.map { if (it.isEmpty) emptyShell() else Shell.shellOf(it.toByteArray()) }
}

fun emptyFavorites(): FavoriteMap = Slots<EggIdFavorites>().apply { iterator().forEach { it.set(EggIdFavorites()) } }
fun emptyMemory(): MemoryMap = Slots<EggIdMemory>().apply { iterator().forEach { it.set(EggIdMemory()) } }
