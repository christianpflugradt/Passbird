package de.pflugradts.passbird.adapter.userinterface

import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.BLUE
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.GREEN
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.ORANGE
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.PURPLE
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.RED
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.WHITE
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.YELLOW

private fun mapColorCode(outputFormatting: OutputFormatting) = when (outputFormatting) {
    WHITE -> 231
    YELLOW -> 220
    ORANGE -> 208
    RED -> 196
    PURPLE -> 207
    BLUE -> 39
    GREEN -> 118
}
fun beginEscape(outputFormatting: OutputFormatting) = print("\u001B[38;5;${mapColorCode(outputFormatting)}m")
fun endEscape() = print("\u001B[0m")
