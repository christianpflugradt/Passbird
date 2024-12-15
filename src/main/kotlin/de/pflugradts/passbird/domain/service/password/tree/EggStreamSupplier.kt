package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.MutableOption.Companion.emptyOption
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.slot.Slot
import java.util.function.Supplier
import java.util.stream.Stream

class EggStreamSupplier(
    private val delegate: Supplier<Stream<Egg>>,
    private val memory: Map<Slot, List<Option<EncryptedShell>>> = emptyMemory(),
) : Supplier<Stream<Egg>> by delegate {
    fun memory() = memory.mapValues { (_, values) -> values.map { it.map { option -> option.copy() } } }
}

fun emptyMemory(): Map<Slot, List<Option<EncryptedShell>>> = enumValues<Slot>().associateWith { List(10) { emptyOption() } }
