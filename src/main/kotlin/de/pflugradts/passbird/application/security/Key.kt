package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.transfer.Bytes

class Key(val secret: Bytes, val iv: Bytes)
