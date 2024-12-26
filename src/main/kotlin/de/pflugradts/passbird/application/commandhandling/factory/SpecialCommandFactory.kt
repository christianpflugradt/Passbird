package de.pflugradts.passbird.application.commandhandling.factory

import de.pflugradts.passbird.application.commandhandling.CommandVariant
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.transfer.Input

abstract class SpecialCommandFactory {
    fun constructFromInput(input: Input) = if (input.command.size > MAX_COMMAND_SIZE) {
        throw IllegalArgumentException("Command parameter not supported: ${input.command.slice(2).asString()}")
    } else {
        internalConstruct(input) ?: NullCommand()
    }
    protected abstract fun internalConstruct(input: Input): Command?
    companion object {
        @JvmStatic
        protected val MAX_COMMAND_SIZE = 3

        @JvmStatic
        protected fun Input.hasNoData() = data.isEmpty

        @JvmStatic
        protected fun Input.hasData() = data.isNotEmpty

        @JvmStatic
        protected fun Shell.size1() = size == 1

        @JvmStatic
        protected fun Shell.size2() = size == 2

        @JvmStatic
        protected fun Shell.size3() = size == 3

        @JvmStatic
        protected fun Shell.isSlotted() = getChar(1).isDigit() || getChar(2).isDigit()

        @JvmStatic
        protected fun Shell.getSlot() = if (getChar(1).isDigit()) slotAt(getChar(1)) else slotAt(getChar(2))

        @JvmStatic
        protected fun Shell.isAddVariant() = getChar(1) == CommandVariant.ADD.value

        @JvmStatic
        protected fun Shell.isDiscardVariant() = getChar(1) == CommandVariant.DISCARD.value

        @JvmStatic
        protected fun Shell.isInfoVariant() = getChar(1) == CommandVariant.INFO.value

        @JvmStatic
        protected fun Shell.isShowAllVariant() = getChar(1) == CommandVariant.SHOW_ALL.value
    }
}
