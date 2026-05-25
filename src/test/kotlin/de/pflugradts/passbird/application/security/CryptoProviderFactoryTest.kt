package de.pflugradts.passbird.application.security

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA

class CryptoProviderFactoryTest {

    private val keyStoreAuthenticationService = mockk<KeyStoreAuthenticationService>()
    private val systemOperation = mockk<SystemOperation>()
    private val cryptoProviderFactory = CryptoProviderFactory(
        keyStoreAuthenticationService = keyStoreAuthenticationService,
        systemOperation = systemOperation,
    )

    @Test
    fun `should create crypto provider`() {
        // given
        every { keyStoreAuthenticationService.authenticate(any(), any()) } returns success(value = createTestKeyShell())

        // when
        val actual = cryptoProviderFactory.createCryptoProvider()

        // then
        expectThat(actual).isA<AesGcmCipher>()
    }

    @Test
    fun `should terminate application after failed authentication`() {
        // given
        every { keyStoreAuthenticationService.authenticate(any(), any()) } returns failure(ex = RuntimeException())

        // when
        tryCatching { cryptoProviderFactory.createCryptoProvider() }

        // then
        verify(exactly = 1) { systemOperation.exit() }
    }
}
