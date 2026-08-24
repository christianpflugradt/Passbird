package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.shell.EncryptedShell

class Yolk private constructor(
    private var secret: EncryptedShell,
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
) : DomainEntity {
    fun viewSecret() = secret.copy()

    companion object {
        fun createYolk(secret: EncryptedShell, algorithm: String = "SHA1", digits: Int = 6, periodSeconds: Int = 30): Yolk {
            require(algorithm.isNotBlank()) { "Missing algorithm" }
            require(digits == 6 || digits == 8) { "Unsupported digits" }
            require(periodSeconds > 0) { "Unsupported period" }
            return Yolk(secret.copy(), algorithm, digits, periodSeconds)
        }
    }
}
