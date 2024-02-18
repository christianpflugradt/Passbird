package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.NoArgumentCommand

class ExportCommand : NoArgumentCommand
class HelpCommand : NoArgumentCommand
class ImportCommand : NoArgumentCommand
class ListCommand : NoArgumentCommand
class ProteinInfoCommand : NoArgumentCommand
class QuitCommand(val quitReason: QuitReason) : NoArgumentCommand
class SetInfoCommand : NoArgumentCommand
class ViewNestCommand : NoArgumentCommand

class NullCommand : NoArgumentCommand

enum class QuitReason { INACTIVITY, USER }
