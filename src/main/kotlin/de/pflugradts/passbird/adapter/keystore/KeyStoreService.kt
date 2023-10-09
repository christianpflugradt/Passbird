package de.pflugradts.passbird.adapter.keystore

import com.google.inject.Inject
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.LoginResult
import de.pflugradts.passbird.application.security.Key
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Chars
import de.pflugradts.passbird.domain.model.transfer.Chars.Companion.charsOf
import java.io.IOException
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

class KeyStoreService @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
) : KeyStoreAdapterPort {

    override fun loadKey(password: Chars, path: Path) = try {
        LoginResult(value = load(password, systemOperation.newInputStream(path)))
    } catch (exception: IOException) {
        LoginResult(exception = exception)
    }

    private fun load(password: Chars, inputStream: InputStream) = inputStream.use {
        val keyStore = systemOperation.jceksInstance
        val passwordChars = password.toCharArray()
        keyStore.load(it, passwordChars)
        val secret = keyStore.getKey(SECRET_ALIAS, passwordChars)
        val iv = keyStore.getKey(IV_ALIAS, passwordChars)
        charsOf(passwordChars).scramble()
        password.scramble()
        Key(bytesOf(secret.encoded), bytesOf(iv.encoded))
    }

    override fun storeKey(password: Chars, path: Path) = store(password, systemOperation.newOutputStream(path))

    private fun store(password: Chars, outputStream: OutputStream) = outputStream.use {
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
        charsOf(passwordChars).scramble()
        password.scramble()
    }
}
