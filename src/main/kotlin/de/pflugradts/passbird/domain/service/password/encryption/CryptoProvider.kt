package de.pflugradts.passbird.domain.service.password.encryption

import de.pflugradts.passbird.domain.model.transfer.Bytes

/**
 * Encrypts/Decrypts [Bytes] using a Key from the
 * [KeyStore][de.pflugradts.passbird.application.KeyStoreAdapterPort].
 */
interface CryptoProvider {
    fun encrypt(bytes: Bytes): Bytes
    fun decrypt(bytes: Bytes): Bytes
}
