package de.pflugradts.passbird.adapter.userinterface

import de.pflugradts.passbird.domain.model.transfer.OutputFormatting

private fun mapColorCode(outputFormatting: OutputFormatting) = when (outputFormatting) {
    OutputFormatting.BLUE -> 34
    OutputFormatting.CYAN -> 36
    OutputFormatting.GREEN -> 32
    OutputFormatting.MAGENTA -> 35
    OutputFormatting.YELLOW -> 33
    OutputFormatting.WHITE -> 37
    OutputFormatting.BRIGHT_BLUE -> 94
    OutputFormatting.BRIGHT_CYAN -> 96
    OutputFormatting.BRIGHT_GREEN -> 92
    OutputFormatting.BRIGHT_MAGENTA -> 95
    OutputFormatting.BRIGHT_YELLOW -> 93
    OutputFormatting.BRIGHT_WHITE -> 97
    else -> 0
}

private fun initEscape(code: Int) = print("\u001B[${code}m")
fun beginEscape(outputFormatting: OutputFormatting) = initEscape(mapColorCode(outputFormatting))
fun endEscape() = initEscape(0)
