package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every

fun fakeCryptoProvider(
    instance: CryptoProvider,
) {
    every { instance.encrypt(any()) } answers { firstArg() }
    every { instance.decrypt(any()) } answers { firstArg() }
}
