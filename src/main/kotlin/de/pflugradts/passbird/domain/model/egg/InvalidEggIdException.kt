package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.transfer.Bytes

class InvalidEggIdException(val eggIdBytes: Bytes) : RuntimeException("Key '${eggIdBytes.asString()}' contains non alphabetic characters!")
