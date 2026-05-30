package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class AesGcmCipherTest {

    private val cryptoProvider = createAesGcmCipherForTesting()

    @Test
    fun `should round trip shell through aes gcm`() {
        val shell = shellOf("passbird")

        val encryptedShell = cryptoProvider.encrypt(shell)

        expectThat(encryptedShell.iv.size) isEqualTo AesGcmCipher.IV_SIZE
        expectThat(cryptoProvider.decrypt(encryptedShell)) isEqualTo shell
    }

    @Test
    fun `should round trip shell through legacy aes gcm`() {
        val shell = shellOf("passbird")

        val encryptedShell = createLegacyAesGcmCipherForTesting().encrypt(shell)

        expectThat(encryptedShell.iv.size) isEqualTo AesGcmCipher.IV_SIZE
        expectThat(createLegacyAesGcmCipherForTesting().decrypt(encryptedShell)) isEqualTo shell
    }

    @Test
    fun `should scramble temporary cipher input bytes after use`() {
        val shell = shellOf("passbird")
        val originalShellBytes = shell.toByteArray().toList()
        lateinit var cipherInputBytes: ByteArray

        val actual = withCipherInputBytes(shell) {
            cipherInputBytes = it
            it.toList()
        }

        expectThat(actual) isEqualTo originalShellBytes
        expectThat(cipherInputBytes.toList()) isNotEqualTo originalShellBytes
        expectThat(shell.toByteArray().toList()) isEqualTo originalShellBytes
    }

    @Test
    fun `should scramble temporary cipher output bytes after use`() {
        val cipherOutputBytes = shellOf("decrypted").toByteArray()
        val originalCipherOutputBytes = cipherOutputBytes.toList()

        val actual = withCipherOutputBytes(cipherOutputBytes) {
            shellOf(it)
        }

        expectThat(actual) isEqualTo shellOf("decrypted")
        expectThat(cipherOutputBytes.toList()) isNotEqualTo originalCipherOutputBytes
    }

    @Test
    fun `should scramble temporary current key bytes after use`() {
        val keyShell = createTestKeyShell()
        val originalKeyBytes = keyShell.toByteArray().toList()
        lateinit var currentKeyBytes: ByteArray

        val actual = withCurrentKeyBytes(keyShell) {
            currentKeyBytes = it
            javax.crypto.spec.SecretKeySpec(it, "AES")
        }

        expectThat(actual.encoded.toList()) isEqualTo originalKeyBytes
        expectThat(currentKeyBytes.toList()) isNotEqualTo originalKeyBytes
    }

    @Test
    fun `should scramble temporary legacy key bytes after use`() {
        lateinit var legacyKeyBytes: ByteArray

        val actual = withLegacyKeyBytes(createTestKeyShell()) {
            legacyKeyBytes = it
            javax.crypto.spec.SecretKeySpec(it, "AES")
        }

        expectThat(actual.encoded.size) isEqualTo 16
        expectThat(legacyKeyBytes.toList()) isNotEqualTo actual.encoded.toList()
    }
}
