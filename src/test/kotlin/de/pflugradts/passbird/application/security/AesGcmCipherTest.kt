package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class AesGcmCipherTest {

    private val cryptoProvider = createAesGcmCipherForTesting()

    @Test
    fun `should round trip shell through aes gcm`() {
        val shell = shellOf("passbird")

        val encryptedShell = cryptoProvider.encrypt(shell)

        expectThat(encryptedShell.iv.size) isEqualTo AesGcmCipher.IV_SIZE
        expectThat(cryptoProvider.decrypt(encryptedShell)) isEqualTo shell
    }
}
