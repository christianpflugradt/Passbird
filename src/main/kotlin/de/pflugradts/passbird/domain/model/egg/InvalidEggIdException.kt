package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.shell.Shell

class InvalidEggIdException(val eggIdShell: Shell) : RuntimeException("Key '${eggIdShell.asString()}' contains non alphabetic characters!")
