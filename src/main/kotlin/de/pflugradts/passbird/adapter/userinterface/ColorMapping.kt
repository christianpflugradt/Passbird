package de.pflugradts.passbird.adapter.userinterface

import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.ERROR_MESSAGE
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.EVENT_HANDLED
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.NEST
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.SPECIAL

private fun mapColorCode(outputFormatting: OutputFormatting) = when (outputFormatting) {
    DEFAULT -> 231
    SPECIAL -> 220
    OPERATION_ABORTED -> 208
    ERROR_MESSAGE -> 196
    HIGHLIGHT -> 207
    NEST -> 39
    EVENT_HANDLED -> 118
}
fun beginEscape(outputFormatting: OutputFormatting) = print("\u001B[38;5;${mapColorCode(outputFormatting)}m")
fun endEscape() = print("\u001B[0m")
