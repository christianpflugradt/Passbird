package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.WHITE

class Output private constructor(val shell: Shell, val formatting: OutputFormatting?) {

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> shell == (other as Output).shell
    }
    override fun hashCode() = shell.hashCode()

    companion object {
        fun outputOf(shell: Shell, formatting: OutputFormatting = WHITE) = Output(shell, formatting)
        fun outputOf(byteArray: ByteArray, formatting: OutputFormatting = WHITE) = outputOf(shellOf(byteArray), formatting)
        fun emptyOutput() = outputOf(emptyShell())
    }
}

enum class OutputFormatting {
    WHITE,
    YELLOW,
    ORANGE,
    RED,
    PURPLE,
    BLUE,
    GREEN,
}
