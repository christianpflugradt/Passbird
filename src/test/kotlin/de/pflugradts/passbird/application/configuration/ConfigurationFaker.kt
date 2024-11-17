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
    withAudibleBellEnabled: Boolean = false,
    withClipboardResetDelaySeconds: Int = 0,
    withClipboardResetEnabled: Boolean = false,
    withConfigurationTemplate: Boolean = false,
    withKeyStoreLocation: String = "",
    withPasswordTreeLocation: String = "",
    withPromptOnRemoval: Boolean = false,
    withPromptOnExportFile: Boolean = false,
    withSpecialCharacters: Boolean = true,
    withPasswordLength: Int = 20,
    withCustomPasswordConfigurations: List<Configuration.CustomPasswordConfiguration> = emptyList(),
    withSecureInputEnabled: Boolean = true,
    withSecureProteinInputEnabled: Boolean = true,
    withPromptForProteinStructureInputToggle: Boolean = false,
    withInactivityTimeLimit: Int = 0,
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
    every { userInterface.audibleBell } returns withAudibleBellEnabled
    val keyStore = mockk<Configuration.KeyStore>()
    every { keyStore.location } returns withKeyStoreLocation
    val passwordTree = mockk<Configuration.PasswordTree>()
    every { passwordTree.location } returns withPasswordTreeLocation
    every { passwordTree.verifyChecksum } returns withVerifyChecksum
    every { passwordTree.verifySignature } returns withVerifySignature
    val adapter = mockk<Configuration.Adapter>()
    every { adapter.clipboard } returns clipboard
    every { adapter.userInterface } returns userInterface
    every { adapter.keyStore } returns keyStore
    every { adapter.passwordTree } returns passwordTree
    every { instance.adapter } returns adapter
    val exchange = mockk<Configuration.Exchange>()
    every { exchange.promptOnExportFile } returns withPromptOnExportFile
    val inactivityLimit = mockk<Configuration.InactivityLimit>()
    every { inactivityLimit.enabled } returns (withInactivityTimeLimit > 0)
    every { inactivityLimit.limitInMinutes } returns withInactivityTimeLimit
    val password = mockk<Configuration.Password>()
    every { password.promptOnRemoval } returns withPromptOnRemoval
    every { password.specialCharacters } returns withSpecialCharacters
    every { password.length } returns withPasswordLength
    every { password.customPasswordConfigurations } returns withCustomPasswordConfigurations
    val application = mockk<Configuration.Application>()
    every { application.exchange } returns exchange
    every { application.inactivityLimit } returns inactivityLimit
    every { application.password } returns password
    every { instance.application } returns application
    val protein = mockk<Configuration.Protein>()
    every { protein.secureProteinStructureInput } returns withSecureProteinInputEnabled
    every { protein.promptForProteinStructureInputToggle } returns withPromptForProteinStructureInputToggle
    val domain = mockk<Configuration.Domain>()
    every { domain.protein } returns protein
    every { instance.domain } returns domain
    every { instance.template } returns withConfigurationTemplate
    every { instance.parsePasswordRequirements() } answers { callOriginal() }
}
