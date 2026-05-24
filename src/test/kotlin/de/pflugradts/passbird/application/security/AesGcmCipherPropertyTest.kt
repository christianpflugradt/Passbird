package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.PROPERTY
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.property.byteContents
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.Tag
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Tag(PROPERTY)
class AesGcmCipherPropertyTest {

    private val cryptoProvider = createAesGcmCipherForTesting()

    @Property
    fun roundTripsArbitraryByteContentThroughAesGcm(@ForAll("plaintexts") plaintext: List<Byte>) {
        val shell = shellOf(plaintext)

        val encryptedShell = cryptoProvider.encrypt(shell)

        expectThat(encryptedShell.iv.size) isEqualTo AesGcmCipher.IV_SIZE
        expectThat(cryptoProvider.decrypt(encryptedShell)) isEqualTo shell
    }

    @Provide
    fun plaintexts() = byteContents()
}
