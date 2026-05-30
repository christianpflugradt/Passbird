package de.pflugradts.passbird.application.commandhandling
import de.pflugradts.passbird.domain.model.shell.Shell
class RememberedCommandMemory {
    private var rememberedCommand: Shell? = null
    fun remember(command: Shell) {
        rememberedCommand?.scramble()
        rememberedCommand = command
    }
    fun view() = rememberedCommand?.copy()
}
