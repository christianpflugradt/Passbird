package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.transfer.Chars.Companion.charsOf
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isTrue
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStoreException

internal class KeyStoreServiceTest {

    private val systemOperation = mockk<SystemOperation>()
    private val keyStoreService = KeyStoreService(systemOperation)

    @Test
    fun `should store key and fail on invalid path`() {
        // given
        val invalidPath = mockk<Path>()

        // when
        val actual = runCatching { keyStoreService.storeKey(charsOf("password".toCharArray()), invalidPath) }

        // then
        expectThat(actual.isFailure).isTrue()
    }

    @Test
    fun `should load key and fail on invalid path`() {
        // given
        val invalidPath = mockk<Path>()

        // when
        val actual = runCatching { keyStoreService.loadKey(charsOf("password".toCharArray()), invalidPath) }

        // then
        expectThat(actual.isFailure).isTrue()
    }

    @Test
    fun `should store key and fail on key store unavailable`() {
        // given
        fakeSystemOperation(
            instance = systemOperation,
            withKeyStoreUnavailable = true,
        )

        // when
        val actual = runCatching { keyStoreService.storeKey(charsOf("password".toCharArray()), Paths.get("")) }

        // then
        expectThat(actual.isFailure).isTrue()
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
        val actual = runCatching { keyStoreService.loadKey(charsOf("password".toCharArray()), Paths.get("")) }

        // then
        expectThat(actual.isFailure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<KeyStoreException>()
    }
}
