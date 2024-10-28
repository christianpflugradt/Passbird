package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.ddd.ValueObject
import de.pflugradts.passbird.domain.model.shell.PlainValue.Companion.plainValueOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.FIRST_SLOT
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.LAST_SLOT
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT

class Input private constructor(val shell: Shell) : ValueObject {

    val command get(): Shell {
        if (shell.size > 0) {
            for (i in 1 until shell.size) {
                if (plainValueOf(shell.getByte(i)).isAlphabeticCharacter) {
                    return shell.slice(0, i)
                }
            }
            return shell
        }
        return emptyShell()
    }

    val data get() = if (shell.size > 1) shell.slice(command.size, shell.size) else emptyShell()
    val isEmpty get() = shell.isEmpty
    val isNotEmpty get() = shell.isNotEmpty
    fun invalidate() = shell.scramble()
    fun extractNestSlot(): Slot = shell.asString().toIntOrNull()?.let {
        if (it in FIRST_SLOT - 1..LAST_SLOT) slotAt(it) else DEFAULT
    } ?: DEFAULT

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> shell == (other as Input).shell
    }

    override fun hashCode() = shell.hashCode()

    companion object {
        fun inputOf(shell: Shell) = Input(shell)
        fun emptyInput() = inputOf(emptyShell())
    }
}
