package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.CharValue.FIRST_DIGIT_INDEX
import de.pflugradts.passbird.domain.model.transfer.CharValue.FIRST_LOWERCASE_INDEX
import de.pflugradts.passbird.domain.model.transfer.CharValue.FIRST_UPPERCASE_INDEX
import de.pflugradts.passbird.domain.model.transfer.CharValue.LAST_DIGIT_INDEX
import de.pflugradts.passbird.domain.model.transfer.CharValue.LAST_LOWERCASE_INDEX
import de.pflugradts.passbird.domain.model.transfer.CharValue.LAST_UPPERCASE_INDEX
import de.pflugradts.passbird.domain.model.transfer.CharValue.MAX_ASCII_VALUE
import de.pflugradts.passbird.domain.model.transfer.CharValue.MIN_ASCII_VALUE
import java.util.concurrent.atomic.AtomicInteger

private val sortReference = buildSortReference()

private fun buildSortReference(): Map<Int, Int> {
    val sortReferenceMap = mutableMapOf<Int, Int>()
    val index = AtomicInteger(0)
    val assignSlot: (Int) -> Unit = { sortReferenceMap[it] = index.getAndIncrement() }
    (MIN_ASCII_VALUE..MAX_ASCII_VALUE).filter { CharValue.of(it).isSymbol }.forEach(assignSlot)
    (FIRST_DIGIT_INDEX..LAST_DIGIT_INDEX).forEach(assignSlot)
    val lowercaseRange = (FIRST_LOWERCASE_INDEX..LAST_LOWERCASE_INDEX)
    val uppercaseRange = (FIRST_UPPERCASE_INDEX..LAST_UPPERCASE_INDEX)
    lowercaseRange.forEachIndexed { i, it ->
        sortReferenceMap[uppercaseRange.elementAt(i)] = index.get()
        sortReferenceMap[lowercaseRange.elementAt(i)] = index.getAndIncrement()
    }
    return sortReferenceMap.toMap()
}

class BytesComparator : Comparator<Bytes> {
    override fun compare(bytes1: Bytes, bytes2: Bytes): Int {
        if (bytes1 == bytes2) return 0
        val reverse = bytes1.size > bytes2.size
        val b1 = if (reverse) bytes2.toByteArray() else bytes1.toByteArray()
        val b2 = if (reverse) bytes1.toByteArray() else bytes2.toByteArray()
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
