package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.application.failure.LoginFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class CryptoProviderFactory @Inject constructor(
    private val keyStoreAuthenticationService: KeyStoreAuthenticationService,
    private val systemOperation: SystemOperation,
) {
    fun createCryptoProvider() = keyStoreAuthenticationService.authenticate(maxAttempts = 3)
        .map { AesGcmCipher(it) }
        .onFailure {
            reportFailure(LoginFailure(3))
            systemOperation.exit()
        }
        .getOrNull()!!
}
