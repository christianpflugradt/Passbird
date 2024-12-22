package de.pflugradts.passbird.domain.model.slot

import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.slot.Slot.S6
import de.pflugradts.passbird.domain.model.slot.Slot.S7
import de.pflugradts.passbird.domain.model.slot.Slot.S8
import de.pflugradts.passbird.domain.model.slot.Slot.S9

open class Slots<T> : Iterable<MutableOption<T>> {

    private val items: Array<MutableOption<T>> = Array(10) { MutableOption.mutableOptionOf() }

    operator fun get(index: Int) = require(index in 0 until 10) { "Index must be between 0 and 9" }
        .run { items[index] }
    operator fun get(slot: Slot) = get(slot.index())

    operator fun set(index: Int, value: T) = require(index in 0 until 10) { "Index must be between 0 and 9" }
        .run { items[index].set(value) }
    operator fun set(slot: Slot, value: T) = set(slot.index(), value)
    operator fun set(index: Int, valueOption: Option<T>) = valueOption.ifPresent { set(index, it) }
    operator fun set(slot: Slot, valueOption: Option<T>) = set(slot.index(), valueOption)

    override operator fun iterator() = items.iterator()

    fun copyUsing(copyFunction: (T) -> T): Slots<T> = Slots<T>().apply {
        this@Slots.items.forEachIndexed { index, item -> this[index] = item.map(copyFunction) }
    }

    companion object {
        fun slotIterator(): Iterable<Slot> = slots
    }
}

private val slots: Iterable<Slot> = listOf(DEFAULT, S1, S2, S3, S4, S5, S6, S7, S8, S9)
