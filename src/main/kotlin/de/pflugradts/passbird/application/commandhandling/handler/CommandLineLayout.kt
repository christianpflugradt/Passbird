package de.pflugradts.passbird.application.commandhandling.handler

import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.domain.model.transfer.Output

private const val INDENT = "    "
private const val COMMAND_COLUMN_WIDTH = 11
private const val ACTION_COLUMN_WIDTH = 14
private const val LABEL_COLUMN_WIDTH = 5
private const val CONFIG_LABEL_COLUMN_WIDTH = 10
private const val STAT_LABEL_COLUMN_WIDTH = 24

internal fun CanPrintInfo.commandInfoOutputs(
    command: String,
    action: String? = null,
    description: String,
    commandColumnWidth: Int = COMMAND_COLUMN_WIDTH,
    actionColumnWidth: Int = ACTION_COLUMN_WIDTH,
): Array<Output> = arrayOf(
    outBold("\n$INDENT${command.padEnd(commandColumnWidth)} "),
    out((action ?: "").padEnd(actionColumnWidth)),
    out(description),
)

internal fun paddedLabel(label: String, columnWidth: Int = LABEL_COLUMN_WIDTH): String = "$INDENT${label.padEnd(columnWidth)}"

internal fun labeledValueLine(label: String, value: String, labelColumnWidth: Int = LABEL_COLUMN_WIDTH): String =
    "${paddedLabel(label, labelColumnWidth)}$value"

internal fun configValueLine(label: String, value: String): String = labeledValueLine(label, value, CONFIG_LABEL_COLUMN_WIDTH)

internal fun statLabel(label: String): String = paddedLabel("$label:", STAT_LABEL_COLUMN_WIDTH)
