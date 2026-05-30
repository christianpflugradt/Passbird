package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.shell.PlainValue.Companion.plainValueOf
import de.pflugradts.passbird.domain.model.shell.Shell

abstract class EggIdException(message: String) : RuntimeException(message)
class EggIdAlreadyExistsException(val eggIdShell: Shell) : EggIdException("EggId '${eggIdShell.asString()}' already exists")
class InvalidEggIdException(val eggIdShell: Shell) : EggIdException("EggId '${eggIdShell.asString()}' contains non alphabetic characters")

fun requireValidEggId(shell: Shell) {
    if (shell.isEmpty || plainValueOf(shell.getByte(0)).isDigit || shell.hasSymbol()) {
        throw InvalidEggIdException(shell)
    }
}

private fun Shell.hasSymbol(): Boolean {
    val shell = copy()
    return try {
        shell.stream().anyMatch { plainValueOf(it).isSymbol }
    } finally {
        shell.scramble()
    }
}
