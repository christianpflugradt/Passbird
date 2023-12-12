package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.shell.Shell

class Key(val secret: Shell, val iv: Shell)
