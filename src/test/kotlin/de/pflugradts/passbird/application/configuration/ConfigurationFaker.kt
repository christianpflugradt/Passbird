package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.configuration.Configuration.AnsiEscapeCodes
import de.pflugradts.passbird.application.configuration.Configuration.Clipboard
import de.pflugradts.passbird.application.configuration.Configuration.ClipboardNativeTooling
import de.pflugradts.passbird.application.configuration.Configuration.ClipboardReset
import de.pflugradts.passbird.application.configuration.Configuration.ProteinTemplate
import de.pflugradts.passbird.application.configuration.Configuration.UserInterface
import io.mockk.every
import io.mockk.mockk

fun fakeConfiguration(
    instance: Configuration,
    withAnsiEscapeCodesEnabled: Boolean = false,
    withAudibleBellEnabled: Boolean = false,
    withClipboardNativeToolingEnabled: Boolean = true,
    withClipboardResetDelaySeconds: Int = 0,
    withClipboardResetEnabled: Boolean = false,
    withConfigurationTemplate: Boolean = false,
    withKeyStoreLocation: String = "",
    withPasswordTreeLocation: String = "",
    withPromptOnRemoval: Boolean = false,
    withPromptOnExportFile: Boolean = false,
    withYolkAlgorithm: String = "SHA1",
    withYolkCopyToClipboard: Boolean = true,
    withYolkDigits: Int = 6,
    withYolkPeriodSeconds: Int = 30,
    withSpecialCharacters: Boolean = true,
    withPasswordLength: Int = 20,
    withCustomPasswordConfigurations: List<Configuration.CustomPasswordConfiguration> = emptyList(),
    withSecureInputEnabled: Boolean = true,
    withSecureProteinInputEnabled: Boolean = true,
    withPromptForProteinStructureInputToggle: Boolean = false,
    withProteinTemplates: List<ProteinTemplate> = emptyList(),
    withInactivityTimeLimit: Int = 0,
    withVerifyChecksum: Boolean = true,
    withVerifySignature: Boolean = true,
    withEggIdMemoryEnabled: Boolean = false,
    withEggIdMemoryPersisted: Boolean = false,
) {
    every { instance.adapter } returns fakeAdapter(
        withAnsiEscapeCodesEnabled = withAnsiEscapeCodesEnabled,
        withAudibleBellEnabled = withAudibleBellEnabled,
        withClipboardNativeToolingEnabled = withClipboardNativeToolingEnabled,
        withClipboardResetDelaySeconds = withClipboardResetDelaySeconds,
        withClipboardResetEnabled = withClipboardResetEnabled,
        withKeyStoreLocation = withKeyStoreLocation,
        withPasswordTreeLocation = withPasswordTreeLocation,
        withSecureInputEnabled = withSecureInputEnabled,
        withVerifyChecksum = withVerifyChecksum,
        withVerifySignature = withVerifySignature,
    )
    every { instance.application } returns fakeApplication(
        withPromptOnRemoval = withPromptOnRemoval,
        withPromptOnExportFile = withPromptOnExportFile,
        withYolkAlgorithm = withYolkAlgorithm,
        withYolkCopyToClipboard = withYolkCopyToClipboard,
        withYolkDigits = withYolkDigits,
        withYolkPeriodSeconds = withYolkPeriodSeconds,
        withSpecialCharacters = withSpecialCharacters,
        withPasswordLength = withPasswordLength,
        withCustomPasswordConfigurations = withCustomPasswordConfigurations,
        withInactivityTimeLimit = withInactivityTimeLimit,
    )
    every { instance.domain } returns fakeDomain(
        withSecureProteinInputEnabled = withSecureProteinInputEnabled,
        withPromptForProteinStructureInputToggle = withPromptForProteinStructureInputToggle,
        withProteinTemplates = withProteinTemplates,
        withEggIdMemoryEnabled = withEggIdMemoryEnabled,
        withEggIdMemoryPersisted = withEggIdMemoryPersisted,
    )
    every { instance.template } returns withConfigurationTemplate
    every { instance.parsePasswordRequirements() } answers { callOriginal() }
}

private fun fakeAdapter(
    withAnsiEscapeCodesEnabled: Boolean,
    withAudibleBellEnabled: Boolean,
    withClipboardNativeToolingEnabled: Boolean,
    withClipboardResetDelaySeconds: Int,
    withClipboardResetEnabled: Boolean,
    withKeyStoreLocation: String,
    withPasswordTreeLocation: String,
    withSecureInputEnabled: Boolean,
    withVerifyChecksum: Boolean,
    withVerifySignature: Boolean,
): Configuration.Adapter {
    val clipboardNativeTooling = mockk<ClipboardNativeTooling>()
    every { clipboardNativeTooling.enabled } returns withClipboardNativeToolingEnabled
    val clipboardReset = mockk<ClipboardReset>()
    every { clipboardReset.enabled } returns withClipboardResetEnabled
    every { clipboardReset.delaySeconds } returns withClipboardResetDelaySeconds
    val clipboard = mockk<Clipboard>()
    every { clipboard.nativeTooling } returns clipboardNativeTooling
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
    return mockk<Configuration.Adapter>().also {
        every { it.clipboard } returns clipboard
        every { it.userInterface } returns userInterface
        every { it.keyStore } returns keyStore
        every { it.passwordTree } returns passwordTree
    }
}

private fun fakeApplication(
    withPromptOnRemoval: Boolean,
    withPromptOnExportFile: Boolean,
    withYolkAlgorithm: String,
    withYolkCopyToClipboard: Boolean,
    withYolkDigits: Int,
    withYolkPeriodSeconds: Int,
    withSpecialCharacters: Boolean,
    withPasswordLength: Int,
    withCustomPasswordConfigurations: List<Configuration.CustomPasswordConfiguration>,
    withInactivityTimeLimit: Int,
): Configuration.Application {
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
    val yolk = mockk<Configuration.Yolk>()
    every { yolk.algorithm } returns withYolkAlgorithm
    every { yolk.copyToClipboard } returns withYolkCopyToClipboard
    every { yolk.digits } returns withYolkDigits
    every { yolk.periodSeconds } returns withYolkPeriodSeconds
    return mockk<Configuration.Application>().also {
        every { it.exchange } returns exchange
        every { it.inactivityLimit } returns inactivityLimit
        every { it.password } returns password
        every { it.yolk } returns yolk
    }
}

private fun fakeDomain(
    withSecureProteinInputEnabled: Boolean,
    withPromptForProteinStructureInputToggle: Boolean,
    withProteinTemplates: List<ProteinTemplate>,
    withEggIdMemoryEnabled: Boolean,
    withEggIdMemoryPersisted: Boolean,
): Configuration.Domain {
    val protein = mockk<Configuration.Protein>()
    every { protein.secureProteinStructureInput } returns withSecureProteinInputEnabled
    every { protein.promptForProteinStructureInputToggle } returns withPromptForProteinStructureInputToggle
    every { protein.templates } returns withProteinTemplates
    val eggIdMemory = mockk<Configuration.EggIdMemory>()
    every { eggIdMemory.enabled } returns withEggIdMemoryEnabled
    every { eggIdMemory.persisted } returns withEggIdMemoryPersisted
    return mockk<Configuration.Domain>().also {
        every { it.protein } returns protein
        every { it.eggIdMemory } returns eggIdMemory
    }
}
