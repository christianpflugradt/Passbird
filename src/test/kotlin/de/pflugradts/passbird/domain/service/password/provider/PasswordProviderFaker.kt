package de.pflugradts.passbird.domain.service.password.provider

import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import io.mockk.every

fun fakePasswordProvider(
    instance: PasswordProvider,
    withCreatedPassword: Bytes = bytesOf("password"),
) {
    every { instance.createNewPassword(any()) } returns withCreatedPassword
}
