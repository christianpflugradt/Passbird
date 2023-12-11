package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.transfer.Bytes

class KeyAlreadyExistsException(val keyBytes: Bytes) : RuntimeException("Key '${keyBytes.asString()}' already exists!")
