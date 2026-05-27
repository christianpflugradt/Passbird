package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import java.io.ByteArrayInputStream
import java.nio.file.Paths
import java.security.Key
import java.security.KeyStore

class KeyStorePersistenceTest {

    private val systemOperation = mockk<SystemOperation>()
    private val keyStorePersistence = KeyStorePersistence(systemOperation)

    @Test
    fun `should scramble loaded key bytes after creating shell`() {
        val path = Paths.get("passbird.sec")
        val password = plainShellOf("Password".toCharArray())
        val expectedKeyBytes = "1234567890abcdef".toByteArray()
        val loadedKeyBytes = expectedKeyBytes.clone()
        val loadedKey = object : Key {
            override fun getAlgorithm() = "AES"
            override fun getFormat() = "RAW"
            override fun getEncoded() = loadedKeyBytes
        }
        val keyStore = mockk<KeyStore>()
        every { systemOperation.newInputStream(path) } returns ByteArrayInputStream(byteArrayOf())
        every { keyStore.load(any(), any<CharArray>()) } returns Unit
        every { keyStore.getKey(SECRET_ALIAS, any()) } returns loadedKey

        val actual = keyStorePersistence.loadKey(openKeyStore = { keyStore }, password = password, path = path)

        expectThat(actual.success).isTrue()
        expectThat(actual.getOrNull()) isEqualTo shellOf(expectedKeyBytes)
        expectThat(loadedKeyBytes.toList()) isNotEqualTo expectedKeyBytes.toList()
        expectThat(loadedKeyBytes.contentEquals(expectedKeyBytes)).isFalse()
        expectThat(password.toCharArray()) isNotEqualTo "Password".toCharArray()
    }
}
