package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.shell.Shell

abstract class EggIdException(message: String) : RuntimeException(message)
class EggIdAlreadyExistsException(val eggIdShell: Shell) : EggIdException("EggId '${eggIdShell.asString()}' already exists")
class InvalidEggIdException(val eggIdShell: Shell) : EggIdException("EggId '${eggIdShell.asString()}' contains non alphabetic characters")
