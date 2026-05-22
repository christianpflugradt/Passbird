package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.commandhandling.command.base.Command

interface CommandBus {
    fun post(command: Command)
}
