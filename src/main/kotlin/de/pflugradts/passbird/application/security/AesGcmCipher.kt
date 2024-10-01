package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val ENCRYPTION_ALGORITHM = "AES"
private const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/GCM/NoPadding"
private const val SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val TAG_LENGTH_BIT = 128
private const val AES_KEY_LENGTH_BIT = 128
private val SALT = "PassbirdSalt2024".toByteArray()

class AesGcmCipher(keyShell: Shell) : CryptoProvider {
    private val secureRandom = SecureRandom()
    private val secretKeySpec = createSecretKeySpec(keyShell)

    override fun encrypt(shell: Shell) = requestSecureIv().let {
        EncryptedShell(payload = cipherize(ENCRYPT_MODE, shell, it.toByteArray()), iv = it)
    }

    override fun decrypt(encryptedShell: EncryptedShell) = cipherize(DECRYPT_MODE, encryptedShell.payload, encryptedShell.iv.toByteArray())

    private fun requestSecureIv(): Shell = ByteArray(IV_SIZE).apply { secureRandom.nextBytes(this) }.toShell()

    private fun cipherize(mode: Int, shell: Shell, iv: ByteArray) = Cipher.getInstance(TRANSFORMATION)
        .apply { init(mode, secretKeySpec, GCMParameterSpec(TAG_LENGTH_BIT, iv)) }
        .doFinal(shell.toByteArray()).toShell()

    private fun createSecretKeySpec(keyShell: Shell) = SecretKeySpec(
        SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
            .generateSecret(PBEKeySpec(keyShell.toPlainShell().toCharArray(), SALT, 100, AES_KEY_LENGTH_BIT)).encoded,
        ENCRYPTION_ALGORITHM,
    )

    private fun ByteArray.toShell() = shellOf(this)

    companion object {
        const val IV_SIZE = 12
    }
}
