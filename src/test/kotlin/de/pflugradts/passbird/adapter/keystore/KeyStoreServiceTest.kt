package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStoreException

class KeyStoreServiceTest {

    private val systemOperation = mockk<SystemOperation>()
    private val keyStoreService = KeyStoreService(systemOperation)

    @Test
    fun `should store key and fail on invalid path`() {
        // given
        val invalidPath = mockk<Path>()
        val password = plainShellOf("Password".toCharArray())

        // when
        val actual = tryCatching { keyStoreService.storeKey(password, invalidPath) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(password.toCharArray()) isNotEqualTo "Password".toCharArray()
    }

    @Test
    fun `should store existing key and fail on invalid path`() {
        // given
        val invalidPath = mockk<Path>()
        val password = plainShellOf("Password".toCharArray())
        val key = shellOf("existing-key")

        // when
        val actual = tryCatching { keyStoreService.storeExistingKey(key, password, invalidPath) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(password.toCharArray()) isNotEqualTo "Password".toCharArray()
        expectThat(key.asString()) isNotEqualTo "existing-key"
    }

    @Test
    fun `should load key and fail on invalid path`() {
        // given
        val invalidPath = mockk<Path>()
        val password = plainShellOf("Password".toCharArray())

        // when
        val actual = keyStoreService.loadKey(password, invalidPath)

        // then
        expectThat(actual.failure).isTrue()
        expectThat(password.toCharArray()) isNotEqualTo "Password".toCharArray()
    }

    @Test
    fun `should store key and fail on key store unavailable`() {
        // given
        val password = plainShellOf("Password".toCharArray())
        fakeSystemOperation(
            instance = systemOperation,
            withKeyStoreUnavailable = true,
        )

        // when
        val actual = tryCatching { keyStoreService.storeKey(password, Paths.get("")) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<KeyStoreException>()
        expectThat(password.toCharArray()) isNotEqualTo "Password".toCharArray()
    }

    @Test
    fun `should store existing key and fail on key store unavailable`() {
        // given
        val password = plainShellOf("Password".toCharArray())
        val key = shellOf("existing-key")
        fakeSystemOperation(
            instance = systemOperation,
            withKeyStoreUnavailable = true,
        )

        // when
        val actual = tryCatching { keyStoreService.storeExistingKey(key, password, Paths.get("")) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<KeyStoreException>()
        expectThat(password.toCharArray()) isNotEqualTo "Password".toCharArray()
        expectThat(key.asString()) isNotEqualTo "existing-key"
    }

    @Test
    fun `should load key and fail on key store unavailable`() {
        // given
        val password = plainShellOf("Password".toCharArray())
        fakeSystemOperation(
            instance = systemOperation,
            withKeyStoreUnavailable = true,
        )

        // when
        val actual = keyStoreService.loadKey(password, Paths.get(""))

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<KeyStoreException>()
        expectThat(password.toCharArray()) isNotEqualTo "Password".toCharArray()
    }
}
