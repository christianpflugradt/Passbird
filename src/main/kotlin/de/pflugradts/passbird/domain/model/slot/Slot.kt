package de.pflugradts.passbird.domain.model.slot

import de.pflugradts.kotlinextensions.tryCatching

enum class Slot {
    DEFAULT,
    S1,
    S2,
    S3,
    S4,
    S5,
    S6,
    S7,
    S8,
    S9,
    ;

    fun index() = (FIRST_SLOT..LAST_SLOT).find { slotAt(it) === this } ?: DEFAULT_INDEX

    companion object {
        const val CAPACITY = 9
        const val FIRST_SLOT = 1
        const val LAST_SLOT = 9
        private const val DEFAULT_INDEX = 0
        fun slotAt(index: String) = slotAt(if (index.length == 1) index[0] else 0.toChar())
        fun slotAt(index: Char) = tryCatching { slotAt(index.toString().toInt()) } getOrElse DEFAULT
        fun slotAt(index: Int) = if (index in FIRST_SLOT..LAST_SLOT) entries[index] else DEFAULT
    }
}
