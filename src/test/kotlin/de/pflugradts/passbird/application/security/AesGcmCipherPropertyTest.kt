package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.PROPERTY
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.property.byteContents
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Tag(PROPERTY)
class AesGcmCipherPropertyTest {

    private val cryptoProvider = createAesGcmCipherForTesting()

    @Test
    fun roundTripsArbitraryByteContentThroughAesGcm() {
        runBlocking {
            checkAll(50, byteContents()) { plaintext ->
                val shell = shellOf(plaintext)

                val encryptedShell = cryptoProvider.encrypt(shell)

                expectThat(encryptedShell.iv.size) isEqualTo AesGcmCipher.IV_SIZE
                expectThat(cryptoProvider.decrypt(encryptedShell)) isEqualTo shell
            }
        }
    }
}
