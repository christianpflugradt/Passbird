package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.BRIGHT_WHITE

class Output private constructor(val shell: Shell, val formatting: OutputFormatting) {

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> shell == (other as Output).shell
    }
    override fun hashCode() = shell.hashCode()

    companion object {
        fun outputOf(shell: Shell, formatting: OutputFormatting = BRIGHT_WHITE) = Output(shell, formatting)
        fun outputOf(byteArray: ByteArray, formatting: OutputFormatting = BRIGHT_WHITE) = outputOf(shellOf(byteArray), formatting)
        fun emptyOutput() = outputOf(emptyShell())
    }
}

enum class OutputFormatting {
    IMPLIED,
    BLUE,
    CYAN,
    GREEN,
    MAGENTA,
    YELLOW,
    WHITE,
    BRIGHT_BLUE,
    BRIGHT_CYAN,
    BRIGHT_GREEN,
    BRIGHT_MAGENTA,
    BRIGHT_YELLOW,
    BRIGHT_WHITE,
}
