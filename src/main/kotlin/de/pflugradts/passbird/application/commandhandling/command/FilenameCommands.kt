package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractFilenameCommand
import de.pflugradts.passbird.domain.model.transfer.Input

class ExportCommand(input: Input) : AbstractFilenameCommand(input)
class ImportCommand(input: Input) : AbstractFilenameCommand(input)
