package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.application.util.ByteArrayUtils
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val AES_CBC_PKCS5PADDING_CIPHER = "AES/CBC/PKCS5Padding"
private const val AES_ENCRYPTION = "AES"
private const val BLOCK_SIZE = 16
private const val SHA512_HASH = "SHA-512"

class Cipherizer internal constructor(private val keyBytes: Bytes, private val ivBytes: Bytes) : CryptoProvider {
    override fun encrypt(bytes: Bytes) = cipherize(Cipher.ENCRYPT_MODE, pack(bytes))

    override fun decrypt(bytes: Bytes) = unpack(cipherize(Cipher.DECRYPT_MODE, bytes))

    private fun cipherize(mode: Int, bytes: Bytes): Bytes {
        val cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING_CIPHER)
        cipher.init(
            mode,
            SecretKeySpec(MessageDigest.getInstance(SHA512_HASH).digest(keyBytes.toByteArray()).copyOf(BLOCK_SIZE), AES_ENCRYPTION),
            IvParameterSpec(ivBytes.toByteArray()),
        )
        return bytesOf(cipher.doFinal(bytes.toByteArray()))
    }

    private fun calcPadding(bytes: Bytes) = BLOCK_SIZE - bytes.size % BLOCK_SIZE

    private fun pack(bytes: Bytes): Bytes {
        val padding = calcPadding(bytes)
        val target = ByteArray(bytes.size + padding)
        ByteArrayUtils.copyBytes(bytes, target, 0)
        target[target.size - 1] = padding.toByte()
        return bytesOf(target)
    }

    private fun unpack(bytes: Bytes): Bytes {
        val padding = bytes.getByte(bytes.size - 1).toInt()
        val target = ByteArray(bytes.size - padding)
        ByteArrayUtils.copyBytes(bytes, target, 0, target.size)
        return bytesOf(target)
    }
}
