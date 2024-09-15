package de.pflugradts.passbird.adapter.keystore

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.security.Key
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import javax.crypto.KeyGenerator

private const val SECRET_ALIAS = "PwMan3Secret"
private const val IV_ALIAS = "PwMan3IV"
private const val AES_ENCRYPTION = "AES"
const val KEYSTORE_KEY_BITS = 128

class KeyStoreService @Inject constructor(private val systemOperation: SystemOperation) : KeyStoreAdapterPort {

    override fun loadKey(password: PlainShell, path: Path) = tryCatching { load(password, systemOperation.newInputStream(path)) }

    private fun load(password: PlainShell, inputStream: InputStream) = inputStream.use {
        val keyStore = systemOperation.jceksInstance
        val passwordChars = password.toCharArray()
        keyStore.load(it, passwordChars)
        val secret = keyStore.getKey(SECRET_ALIAS, passwordChars)
        val iv = keyStore.getKey(IV_ALIAS, passwordChars)
        plainShellOf(passwordChars).scramble()
        password.scramble()
        Key(shellOf(secret.encoded), shellOf(iv.encoded))
    }

    override fun storeKey(password: PlainShell, path: Path) = store(password, systemOperation.newOutputStream(path))

    private fun store(password: PlainShell, outputStream: OutputStream) = outputStream.use {
        val keyStore = systemOperation.jceksInstance
        val passwordChars = password.toCharArray()
        keyStore.load(null, null)
        val keyGenerator = KeyGenerator.getInstance(AES_ENCRYPTION)
        keyGenerator.init(KEYSTORE_KEY_BITS)
        keyStore.setEntry(
            SECRET_ALIAS,
            KeyStore.SecretKeyEntry(keyGenerator.generateKey()),
            PasswordProtection(passwordChars),
        )
        keyStore.setEntry(
            IV_ALIAS,
            KeyStore.SecretKeyEntry(keyGenerator.generateKey()),
            PasswordProtection(passwordChars),
        )
        keyStore.store(outputStream, passwordChars)
        plainShellOf(passwordChars).scramble()
        password.scramble()
    }
}
