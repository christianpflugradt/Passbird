package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.CryptoUtils
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Chars.Companion.charsOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.java.exists
import java.io.File
import java.nio.file.Paths
import java.util.UUID

internal class KeyStoreServiceIT {

    private var keyStoreService: KeyStoreService? = null
    private var tempKeyStoreDirectory: String? = null
    private var keyStoreFile: String? = null

    @BeforeEach
    fun setup() {
        keyStoreService = KeyStoreService(SystemOperation())
        tempKeyStoreDirectory = UUID.randomUUID().toString()
        keyStoreFile = tempKeyStoreDirectory + File.separator + ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(File(tempKeyStoreDirectory!!).mkdir()).isTrue()
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(keyStoreFile!!).delete()).isTrue()
        expectThat(File(tempKeyStoreDirectory!!).delete()).isTrue()
    }

    @Test
    fun `should use key store roundtrip`() {
        // given
        val password = "p4s5wrD"
        val oneTimePasswordChars1 = charsOf(password.toCharArray())
        val oneTimePasswordChars2 = charsOf(password.toCharArray())
        val path = Paths.get(keyStoreFile!!)
        val expectedByteArraySize = CryptoUtils.KEYSTORE_KEY_BITS / 8
        expectThat(oneTimePasswordChars1.toCharArray()) isEqualTo password.toCharArray()
        expectThat(oneTimePasswordChars2.toCharArray()) isEqualTo password.toCharArray()

        // when
        val actualStoreResult = runCatching { keyStoreService!!.storeKey(oneTimePasswordChars1, path) }
        val actualLoadResult = keyStoreService!!.loadKey(oneTimePasswordChars2, path)

        // then
        expectThat(File(keyStoreFile!!)).exists()
        expectThat(actualStoreResult).isSuccess()
        expectThat(actualLoadResult.isSuccess).isTrue()
        expectThat(actualLoadResult.value?.secret?.size) isEqualTo expectedByteArraySize
        expectThat(actualLoadResult.value?.iv?.size) isEqualTo expectedByteArraySize
        expectThat(oneTimePasswordChars1.toCharArray()) isNotEqualTo password.toCharArray()
        expectThat(oneTimePasswordChars2.toCharArray()) isNotEqualTo password.toCharArray()
    }
}