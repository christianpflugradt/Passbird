package de.pflugradts.passbird.application.commandhandling.handler

import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.domain.model.transfer.Output

private const val COMMAND_INDENT = "  "
private const val COMMAND_COLUMN_WIDTH = 18
private const val LABEL_INDENT = "  "
private const val LABEL_COLUMN_WIDTH = 8
private const val STAT_LABEL_COLUMN_WIDTH = 24

internal fun CanPrintInfo.commandInfoOutputs(command: String, description: String): Array<Output> = arrayOf(
    outBold("\n$COMMAND_INDENT${command.padEnd(COMMAND_COLUMN_WIDTH)}"),
    out(description),
)

internal fun paddedLabel(label: String, columnWidth: Int = LABEL_COLUMN_WIDTH): String = "$LABEL_INDENT${label.padEnd(columnWidth)}"

internal fun labeledValueLine(label: String, value: String): String = "$LABEL_INDENT${label.padEnd(LABEL_COLUMN_WIDTH)}$value"

internal fun statLabel(label: String): String = paddedLabel("$label:", STAT_LABEL_COLUMN_WIDTH)
