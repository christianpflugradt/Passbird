package de.pflugradts.passbird.domain.model.namespace

enum class NamespaceSlot {
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

    fun index() = (FIRST_NAMESPACE..LAST_NAMESPACE).find { at(it) === this } ?: DEFAULT_INDEX

    companion object {
        const val CAPACITY = 9
        const val FIRST_NAMESPACE = 1
        const val LAST_NAMESPACE = 9
        private const val DEFAULT_INDEX = 10
        fun at(index: Char) = try { at(index.toString().toInt()) } catch (ex: NumberFormatException) { DEFAULT }
        fun at(index: Int) = if (index in FIRST_NAMESPACE..LAST_NAMESPACE) entries[index] else DEFAULT
    }
}
