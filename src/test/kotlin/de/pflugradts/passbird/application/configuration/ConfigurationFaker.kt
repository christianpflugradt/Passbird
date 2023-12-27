package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.configuration.Configuration.AnsiEscapeCodes
import de.pflugradts.passbird.application.configuration.Configuration.Clipboard
import de.pflugradts.passbird.application.configuration.Configuration.ClipboardReset
import de.pflugradts.passbird.application.configuration.Configuration.UserInterface
import io.mockk.every
import io.mockk.mockk

fun fakeConfiguration(
    instance: Configuration,
    withAnsiEscapeCodesEnabled: Boolean = false,
    withClipboardResetDelaySeconds: Int = 0,
    withClipboardResetEnabled: Boolean = false,
    withConfigurationTemplate: Boolean = false,
    withKeyStoreLocation: String = "",
    withPasswordStoreLocation: String = "",
    withPromptOnRemoval: Boolean = false,
    withSecureInputEnabled: Boolean = true,
    withVerifyChecksum: Boolean = true,
    withVerifySignature: Boolean = true,
) {
    val clipboardReset = mockk<ClipboardReset>()
    every { clipboardReset.enabled } returns withClipboardResetEnabled
    every { clipboardReset.delaySeconds } returns withClipboardResetDelaySeconds
    val clipboard = mockk<Clipboard>()
    every { clipboard.reset } returns clipboardReset
    val ansiEscapeCodes = mockk<AnsiEscapeCodes>()
    every { ansiEscapeCodes.enabled } returns withAnsiEscapeCodesEnabled
    val userInterface = mockk<UserInterface>()
    every { userInterface.secureInput } returns withSecureInputEnabled
    every { userInterface.ansiEscapeCodes } returns ansiEscapeCodes
    val keyStore = mockk<Configuration.KeyStore>()
    every { keyStore.location } returns withKeyStoreLocation
    val passwordStore = mockk<Configuration.PasswordStore>()
    every { passwordStore.location } returns withPasswordStoreLocation
    every { passwordStore.verifyChecksum } returns withVerifyChecksum
    every { passwordStore.verifySignature } returns withVerifySignature
    val adapter = mockk<Configuration.Adapter>()
    every { adapter.clipboard } returns clipboard
    every { adapter.userInterface } returns userInterface
    every { adapter.keyStore } returns keyStore
    every { adapter.passwordStore } returns passwordStore
    every { instance.adapter } returns adapter
    val password = mockk<Configuration.Password>()
    every { password.promptOnRemoval } returns withPromptOnRemoval
    val application = mockk<Configuration.Application>()
    every { application.password } returns password
    every { instance.application } returns application
    every { instance.template } returns withConfigurationTemplate
    every { instance.parsePasswordRequirements() } returns mockk()
}
