package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.kotlinextensions.tryCatching

enum class NestSlot {
    DEFAULT,
    N1,
    N2,
    N3,
    N4,
    N5,
    N6,
    N7,
    N8,
    N9,
    INVALID,
    ;

    fun index() = (FIRST_SLOT..LAST_SLOT).find { nestSlotAt(it) === this } ?: DEFAULT_INDEX

    companion object {
        const val CAPACITY = 9
        const val FIRST_SLOT = 1
        const val LAST_SLOT = 9
        private const val DEFAULT_INDEX = 10
        fun nestSlotAt(index: String) = nestSlotAt(if (index.length == 1) index[0] else 0.toChar())
        fun nestSlotAt(index: Char) = tryCatching { nestSlotAt(index.toString().toInt()) } getOrElse DEFAULT
        fun nestSlotAt(index: Int) = if (index in FIRST_SLOT..LAST_SLOT) entries[index] else DEFAULT
    }
}
