package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.NoArgumentCommand

class ExportCommand(val selective: Boolean = false) : NoArgumentCommand
class HelpCommand : NoArgumentCommand
class ImportCommand(val selective: Boolean = false) : NoArgumentCommand
class ChangeMasterPasswordCommand : NoArgumentCommand
class MemoryInfoCommand : NoArgumentCommand
class ProteinInfoCommand : NoArgumentCommand
class QuitCommand(val quitReason: QuitReason) : NoArgumentCommand
class SetInfoCommand : NoArgumentCommand
class ViewNestCommand : NoArgumentCommand
class ViewMemoryCommand : NoArgumentCommand

class NullCommand : NoArgumentCommand

enum class QuitReason { INACTIVITY, USER }
