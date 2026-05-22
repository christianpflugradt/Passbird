package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every

fun fakeCryptoProvider(instance: CryptoProvider) {
    every { instance.encrypt(any<Shell>()) } answers { firstArg<Shell>().fakeEnc() }
    every { instance.decrypt(any<EncryptedShell>()) } answers { firstArg<EncryptedShell>().fakeDec() }
}
