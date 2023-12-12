package de.pflugradts.passbird.domain.service.password.provider

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import io.mockk.every

fun fakePasswordProvider(
    instance: PasswordProvider,
    withCreatedPassword: Shell = shellOf("password"),
) {
    every { instance.createNewPassword(any()) } returns withCreatedPassword
}
