package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.transfer.Bytes

class EggIdAlreadyExistsException(val eggIdBytes: Bytes) : RuntimeException("Key '${eggIdBytes.asString()}' already exists!")
