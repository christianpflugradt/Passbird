package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.shell.Shell

class EggIdAlreadyExistsException(val eggIdShell: Shell) : RuntimeException("Key '${eggIdShell.asString()}' already exists!")
