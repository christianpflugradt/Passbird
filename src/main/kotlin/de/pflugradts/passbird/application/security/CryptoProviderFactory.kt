package de.pflugradts.passbird.application.security
import de.pflugradts.passbird.application.failure.LoginFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
class CryptoProviderFactory constructor(
    private val keyStoreAuthenticationService: KeyStoreAuthenticationService,
    private val systemOperation: SystemOperation,
) {
    fun createCryptoProvider() = keyStoreAuthenticationService.authenticate(maxAttempts = 3)
        .map { AesGcmCipher(it) }
        .onFailure {
            reportFailure(LoginFailure(3))
            systemOperation.exit(FAILURE_EXIT_STATUS)
        }
        .getOrNull()!!
}
