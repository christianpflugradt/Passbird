package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.slot.Slots
import java.util.function.Supplier
import java.util.stream.Stream

class EggStreamSupplier(
    private val delegate: Supplier<Stream<Egg>>,
    private val memory: MemoryMap = emptyMemory(),
) : Supplier<Stream<Egg>> by delegate {
    fun memory(): MemoryMap = memory.copyUsing { it.copy() }
}

fun emptyMemory(): MemoryMap = Slots<EggIdMemory>().apply { iterator().forEach { it.set(EggIdMemory()) } }
