package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.transfer.Bytes

class InvalidKeyException(val keyBytes: Bytes) : RuntimeException("Key '${keyBytes.asString()}' contains non alphabetic characters!")
