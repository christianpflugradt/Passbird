package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.security.Cipherizer
import de.pflugradts.passbird.application.security.Key
import de.pflugradts.passbird.domain.model.transfer.Chars
import java.nio.file.Path

/**
 * AdapterPort to access a KeyStore to load or store a Key used for symmetric encryption.
 */
class LoginResult(
    val value: Key? = null,
    val exception: Exception? = null,
) {
    init {
        if ((value == null && exception == null) || (value != null && exception != null)) {
            throw IllegalArgumentException("result must contain either a value or an exception")
        }
    }

    val isSuccess get() = value != null
    val isFailure get() = !isSuccess

    fun retry(block: (LoginResult) -> LoginResult) = if (isFailure) block(this) else this

    fun fold(onSuccess: (Key) -> Cipherizer?, onFailure: (Exception) -> Cipherizer?) =
        if (isSuccess) onSuccess(value!!) else onFailure(exception!!)
}

interface KeyStoreAdapterPort {
    fun loadKey(password: Chars, path: Path): LoginResult
    fun storeKey(password: Chars, path: Path)
}
