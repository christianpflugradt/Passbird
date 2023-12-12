package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
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

        // when
        val actual = tryCatching { keyStoreService.storeKey(plainShellOf("password".toCharArray()), invalidPath) }

        // then
        expectThat(actual.failure).isTrue()
    }

    @Test
    fun `should load key and fail on invalid path`() {
        // given
        val invalidPath = mockk<Path>()

        // when
        val actual = keyStoreService.loadKey(plainShellOf("password".toCharArray()), invalidPath)

        // then
        expectThat(actual.failure).isTrue()
    }

    @Test
    fun `should store key and fail on key store unavailable`() {
        // given
        fakeSystemOperation(
            instance = systemOperation,
            withKeyStoreUnavailable = true,
        )

        // when
        val actual = tryCatching { keyStoreService.storeKey(plainShellOf("password".toCharArray()), Paths.get("")) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<KeyStoreException>()
    }

    @Test
    fun `should load key and fail on key store unavailable`() {
        // given
        fakeSystemOperation(
            instance = systemOperation,
            withKeyStoreUnavailable = true,
        )

        // when
        val actual = keyStoreService.loadKey(plainShellOf("password".toCharArray()), Paths.get(""))

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<KeyStoreException>()
    }
}
