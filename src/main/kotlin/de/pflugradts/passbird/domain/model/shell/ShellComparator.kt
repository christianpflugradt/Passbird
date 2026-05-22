package de.pflugradts.passbird.domain.model.shell

import java.util.concurrent.atomic.AtomicInteger

private val sortReference = buildSortReference()

private fun buildSortReference(): Map<Int, Int> {
    val sortReferenceMap = mutableMapOf<Int, Int>()
    val index = AtomicInteger(0)
    val assignSlot: (Int) -> Unit = { sortReferenceMap[it] = index.getAndIncrement() }
    (MIN_ASCII_VALUE..MAX_ASCII_VALUE).filter { PlainValue.plainValueOf(it).isSymbol }.forEach(assignSlot)
    (FIRST_DIGIT_INDEX..LAST_DIGIT_INDEX).forEach(assignSlot)
    val lowercaseRange = (FIRST_LOWERCASE_INDEX..LAST_LOWERCASE_INDEX)
    val uppercaseRange = (FIRST_UPPERCASE_INDEX..LAST_UPPERCASE_INDEX)
    lowercaseRange.forEachIndexed { i, _ ->
        sortReferenceMap[uppercaseRange.elementAt(i)] = index.get()
        sortReferenceMap[lowercaseRange.elementAt(i)] = index.getAndIncrement()
    }
    return sortReferenceMap.toMap()
}

class ShellComparator : Comparator<Shell> {
    override fun compare(shell1: Shell, shell2: Shell): Int {
        if (shell1 == shell2) return 0
        val reverse = shell1.size > shell2.size
        val b1 = if (reverse) shell2.toByteArray() else shell1.toByteArray()
        val b2 = if (reverse) shell1.toByteArray() else shell2.toByteArray()
        var index = -1
        var result = if (b1.size == b2.size) 0 else -1
        while (++index < b1.size) {
            val b1SortIndex = sortReference[b1[index].toInt()]
            val b2SortIndex = sortReference[b2[index].toInt()]
            if (b1SortIndex!!.compareTo(b2SortIndex!!) != 0) {
                result = if (b1SortIndex < b2SortIndex) -1 else 1
                break
            }
        }
        return if (reverse) result * -1 else result
    }
}
