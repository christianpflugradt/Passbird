package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val AES_CBC_PKCS5PADDING_CIPHER = "AES/CBC/PKCS5Padding"
private const val AES_ENCRYPTION = "AES"
private const val BLOCK_SIZE = 16
private const val SHA512_HASH = "SHA-512"

class Cipherizer(private val keyShell: Shell, private val ivShell: Shell) : CryptoProvider {
    override fun encrypt(shell: Shell) = cipherize(Cipher.ENCRYPT_MODE, pack(shell))

    override fun decrypt(shell: Shell) = unpack(cipherize(Cipher.DECRYPT_MODE, shell))

    private fun cipherize(mode: Int, shell: Shell): Shell {
        val cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING_CIPHER)
        cipher.init(
            mode,
            SecretKeySpec(MessageDigest.getInstance(SHA512_HASH).digest(keyShell.toByteArray()).copyOf(BLOCK_SIZE), AES_ENCRYPTION),
            IvParameterSpec(ivShell.toByteArray()),
        )
        return shellOf(cipher.doFinal(shell.toByteArray()))
    }

    private fun calcPadding(shell: Shell) = BLOCK_SIZE - shell.size % BLOCK_SIZE

    private fun pack(shell: Shell): Shell {
        val padding = calcPadding(shell)
        val target = ByteArray(shell.size + padding)
        copyBytes(shell.toByteArray(), target, 0)
        target[target.size - 1] = padding.toByte()
        return shellOf(target)
    }

    private fun unpack(shell: Shell): Shell {
        val padding = shell.getByte(shell.size - 1).toInt()
        val target = ByteArray(shell.size - padding)
        copyBytes(shell.toByteArray(), target, 0, target.size)
        return shellOf(target)
    }
}
