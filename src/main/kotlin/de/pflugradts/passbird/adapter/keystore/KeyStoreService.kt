package de.pflugradts.passbird.adapter.keystore

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.MAX_ASCII_VALUE
import de.pflugradts.passbird.domain.model.shell.MIN_ASCII_VALUE
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.nio.file.Path
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import javax.crypto.KeyGenerator

private const val SECRET_ALIAS = "PwMan3Secret"
private const val ALGORITHM = "AES"
const val KEYSTORE_KEY_BITS = 128

class KeyStoreService @Inject constructor(private val systemOperation: SystemOperation) : KeyStoreAdapterPort {

    override fun loadKey(password: PlainShell, path: Path) = tryCatching {
        val passwordChars = password.toCharArray()
        try {
            systemOperation.newInputStream(path).use {
                val keyStore = systemOperation.jceksInstance
                keyStore.load(it, passwordChars)
                val secret = keyStore.getKey(SECRET_ALIAS, passwordChars)
                shellOf(secret.encoded)
            }
        } finally {
            passwordChars.scramble()
            password.scramble()
        }
    }

    override fun storeKey(password: PlainShell, path: Path) {
        val passwordChars = password.toCharArray()
        try {
            systemOperation.writeToSensitiveFile(path) { outputStream ->
                val keyStore = systemOperation.jceksInstance
                keyStore.load(null, null)
                val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
                keyGenerator.init(KEYSTORE_KEY_BITS)
                keyStore.setEntry(
                    SECRET_ALIAS,
                    KeyStore.SecretKeyEntry(keyGenerator.generateKey()),
                    PasswordProtection(passwordChars),
                )
                keyStore.store(outputStream, passwordChars)
            }
        } finally {
            passwordChars.scramble()
            password.scramble()
        }
    }
}

private fun CharArray.scramble() = indices.forEach {
    this[it] = (PlainShell.SECURE_RANDOM.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toChar()
}
