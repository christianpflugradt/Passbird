package de.pflugradts.passbird.application.commandhandling.capabilities

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.NEST
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.SPECIAL

class CanPrintInfo {
    fun outBold(text: String) = outputOf(shellOf(text), HIGHLIGHT)
    fun outNest(text: String) = outputOf(shellOf(text), NEST)
    fun outSpecial(text: String) = outputOf(shellOf(text), SPECIAL)
    fun out(text: String) = outputOf(shellOf(text), DEFAULT)
}
