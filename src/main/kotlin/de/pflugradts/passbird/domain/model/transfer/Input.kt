package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.ddd.ValueObject
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.FIRST_NAMESPACE
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.LAST_NAMESPACE
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.at
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.INVALID
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.CharValue.Companion.charValueOf

/**
 * An Input represents data given by the user through the
 * [UserInterface][de.pflugradts.passbird.application.UserInterfaceAdapterPort].
 */
class Input private constructor(val bytes: Bytes) : ValueObject {

    val command get(): Bytes {
        if (bytes.size > 0) {
            for (i in 1 until bytes.size) {
                if (charValueOf(bytes.getByte(i)).isAlphabeticCharacter) {
                    return bytes.slice(0, i)
                }
            }
            return bytes
        }
        return emptyBytes()
    }

    val data get() = if (bytes.size > 1) bytes.slice(command.size, bytes.size) else emptyBytes()
    val isEmpty get() = bytes.isEmpty
    fun invalidate() = bytes.scramble()
    fun parseNamespace(): NamespaceSlot = bytes.asString().toIntOrNull()?.let {
        if (it in FIRST_NAMESPACE - 1..LAST_NAMESPACE) at(it) else INVALID
    } ?: INVALID

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> bytes == (other as Input).bytes
    }

    override fun hashCode() = bytes.hashCode()

    companion object {
        fun inputOf(bytes: Bytes) = Input(bytes)
        fun emptyInput() = inputOf(emptyBytes())
    }
}
