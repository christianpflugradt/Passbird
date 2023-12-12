package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell

class Output private constructor(val shell: Shell) {

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> shell == (other as Output).shell
    }
    override fun hashCode() = shell.hashCode()

    companion object {
        fun outputOf(shell: Shell) = Output(shell)
        fun outputOf(byteArray: ByteArray) = outputOf(Shell.shellOf(byteArray))
        fun emptyOutput() = outputOf(emptyShell())
    }
}
