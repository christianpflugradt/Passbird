package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.domain.model.transfer.Input

interface InputHandler {
    fun handleInput(input: Input)
}
