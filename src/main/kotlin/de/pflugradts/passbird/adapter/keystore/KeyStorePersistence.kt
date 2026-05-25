package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.MAX_ASCII_VALUE
import de.pflugradts.passbird.domain.model.shell.MIN_ASCII_VALUE
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.SECURE_RANDOM
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.nio.file.Path
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

internal const val SECRET_ALIAS = "PwMan3Secret"
private const val ALGORITHM = "AES"
const val KEYSTORE_KEY_BITS = 128

internal class KeyStorePersistence(private val systemOperation: SystemOperation) {

    fun loadKey(openKeyStore: () -> KeyStore, password: PlainShell, path: Path) = tryCatching {
        val passwordChars = password.toCharArray()
        try {
            systemOperation.newInputStream(path).use {
                val keyStore = openKeyStore()
                keyStore.load(it, passwordChars)
                val secret = keyStore.getKey(SECRET_ALIAS, passwordChars)
                shellOf(secret.encoded)
            }
        } finally {
            passwordChars.scramble()
            password.scramble()
        }
    }

    fun storeKey(openKeyStore: () -> KeyStore, password: PlainShell, path: Path) {
        val passwordChars = password.toCharArray()
        try {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            keyGenerator.init(KEYSTORE_KEY_BITS)
            persistKey(openKeyStore, keyGenerator.generateKey(), passwordChars, path)
        } finally {
            passwordChars.scramble()
            password.scramble()
        }
    }

    fun storeExistingKey(openKeyStore: () -> KeyStore, key: Shell, password: PlainShell, path: Path) {
        val keyBytes = key.toByteArray()
        val passwordChars = password.toCharArray()
        try {
            persistKey(openKeyStore, SecretKeySpec(keyBytes, ALGORITHM), passwordChars, path)
        } finally {
            keyBytes.scramble()
            key.scramble()
            passwordChars.scramble()
            password.scramble()
        }
    }

    private fun persistKey(openKeyStore: () -> KeyStore, secretKey: SecretKey, passwordChars: CharArray, path: Path) {
        systemOperation.writeToSensitiveFile(path) { outputStream ->
            val keyStore = openKeyStore()
            keyStore.load(null, null)
            keyStore.setEntry(
                SECRET_ALIAS,
                KeyStore.SecretKeyEntry(secretKey),
                PasswordProtection(passwordChars),
            )
            keyStore.store(outputStream, passwordChars)
        }
    }
}

private fun CharArray.scramble() = indices.forEach {
    this[it] = (PlainShell.SECURE_RANDOM.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toChar()
}

private fun ByteArray.scramble() = SECURE_RANDOM.nextBytes(this)
