package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import java.util.Enumeration

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
        val keyStore = TestKeyStore(loadedKey)
        io.mockk.every { systemOperation.newInputStream(path) } returns ByteArrayInputStream(byteArrayOf())

        val actual = keyStorePersistence.loadKey(openKeyStore = { keyStore }, password = password, path = path)

        expectThat(actual.success).isTrue()
        expectThat(actual.getOrNull()) isEqualTo shellOf(expectedKeyBytes)
        expectThat(loadedKeyBytes.toList()) isNotEqualTo expectedKeyBytes.toList()
        expectThat(loadedKeyBytes.contentEquals(expectedKeyBytes)).isFalse()
        expectThat(password.toCharArray()) isNotEqualTo "Password".toCharArray()
    }

    private class TestKeyStore(loadedKey: Key) : KeyStore(TestKeyStoreSpi(loadedKey), null, "test")

    private class TestKeyStoreSpi(private val loadedKey: Key) : KeyStoreSpi() {
        override fun engineGetKey(alias: String?, password: CharArray?) = loadedKey

        override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null

        override fun engineGetCertificate(alias: String?): Certificate? = null

        override fun engineGetCreationDate(alias: String?) = Date(0)

        override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<out Certificate>?) = Unit

        override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) = Unit

        override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) = Unit

        override fun engineDeleteEntry(alias: String?) = Unit

        override fun engineAliases(): Enumeration<String> = Collections.emptyEnumeration()

        override fun engineContainsAlias(alias: String?) = alias == SECRET_ALIAS

        override fun engineSize() = 1

        override fun engineIsKeyEntry(alias: String?) = alias == SECRET_ALIAS

        override fun engineIsCertificateEntry(alias: String?) = false

        override fun engineGetCertificateAlias(cert: Certificate?) = null

        override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit

        override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
    }
}
