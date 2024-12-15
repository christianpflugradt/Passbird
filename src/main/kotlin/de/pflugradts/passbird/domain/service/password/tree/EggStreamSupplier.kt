package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.slot.Slot
import java.util.function.Supplier
import java.util.stream.Stream

class EggStreamSupplier(
    private val delegate: Supplier<Stream<Egg>>,
    private val memory: MemoryMap = emptyMemory(),
) : Supplier<Stream<Egg>> by delegate {
    fun memory() = memory.mapValues { (_, values) -> values.copy() }
}

typealias MemoryList = List<MutableOption<EncryptedShell>>
typealias MemoryMap = Map<Slot, MemoryList>

fun emptyMemory(): MemoryMap = enumValues<Slot>().associateWith { List(10) { mutableOptionOf() } }
fun MemoryList.copy() = map { it.mapMutable { option -> option.copy() } }
