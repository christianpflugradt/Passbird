package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import strikt.java.exists
import java.io.File
import java.nio.file.Paths
import java.util.UUID

@Tag(INTEGRATION)
class KeyStoreServiceIntegrationTest {

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
    fun `should write to and read from key store`() {
        // given
        val password = "p4s5wrD"
        val oneTimePasswordPlainShell1 = plainShellOf(password.toCharArray())
        val oneTimePasswordPlainShell2 = plainShellOf(password.toCharArray())
        val path = Paths.get(keyStoreFile!!)
        val expectedByteArraySize = KEYSTORE_KEY_BITS / 8
        expectThat(oneTimePasswordPlainShell1.toCharArray()) isEqualTo password.toCharArray()
        expectThat(oneTimePasswordPlainShell2.toCharArray()) isEqualTo password.toCharArray()

        // when
        val actualStoreResult = tryCatching { keyStoreService!!.storeKey(oneTimePasswordPlainShell1, path) }
        val actualLoadResult = keyStoreService!!.loadKey(oneTimePasswordPlainShell2, path)

        // then
        expectThat(File(keyStoreFile!!)).exists()
        expectThat(actualStoreResult.success).isTrue()
        expectThat(actualLoadResult.success).isTrue()
        expectThat(actualLoadResult.getOrNull()?.size) isEqualTo expectedByteArraySize
        expectThat(oneTimePasswordPlainShell1.toCharArray()) isNotEqualTo password.toCharArray()
        expectThat(oneTimePasswordPlainShell2.toCharArray()) isNotEqualTo password.toCharArray()
    }
}
