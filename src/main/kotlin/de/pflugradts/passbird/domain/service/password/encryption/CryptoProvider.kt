package de.pflugradts.passbird.domain.service.password.encryption

import de.pflugradts.passbird.domain.model.transfer.Bytes

interface CryptoProvider {
    fun encrypt(bytes: Bytes): Bytes
    fun decrypt(bytes: Bytes): Bytes
}
