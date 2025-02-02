package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.domain.model.egg.PasswordRequirements

private const val DEFAULT_BACKUP_DIRECTORY = "backups"
private const val DEFAULT_CLIPBOARD_RESET_DELAY_SECONDS = 10
private const val DEFAULT_PASSWORD_LENGTH = 20

data class Configuration(
    @Transient override var template: Boolean = false,
    override val application: Application = Application(),
    override val adapter: Adapter = Adapter(),
    override val domain: Domain = Domain(),
) : UpdatableConfiguration {

    override fun parsePasswordRequirements() = PasswordRequirements(
        length = application.password.length,
        hasSpecialCharacters = application.password.specialCharacters,
    )

    override fun updateDirectory(directory: Directory) {
        application.backup.location = "${directory.value}/$DEFAULT_BACKUP_DIRECTORY"
        adapter.keyStore.location = directory.value
        adapter.passwordTree.location = directory.value
    }

    data class Application(
        override val backup: Backup = Backup(),
        override val exchange: Exchange = Exchange(),
        override val inactivityLimit: InactivityLimit = InactivityLimit(),
        override val password: Password = Password(),
    ) : ReadableConfiguration.Application
    data class Backup(
        override var location: String = DEFAULT_BACKUP_DIRECTORY,
        override var numberOfBackups: Int = 10,
        override val configuration: BackupSettings = BackupSettings(),
        override val passwordTree: BackupSettings = BackupSettings(),
        override val keyStore: BackupSettings = BackupSettings(),
    ) : ReadableConfiguration.Backup
    data class BackupSettings(
        override val enabled: Boolean = true,
        override val location: String? = null,
        override val numberOfBackups: Int? = null,
    ) : ReadableConfiguration.BackupSettings
    data class Exchange(
        override val promptOnExportFile: Boolean = true,
    ) : ReadableConfiguration.Exchange
    data class InactivityLimit(
        override val enabled: Boolean = false,
        override val limitInMinutes: Int = 10,
    ) : ReadableConfiguration.InactivityLimit
    data class Password(
        override val length: Int = DEFAULT_PASSWORD_LENGTH,
        override val specialCharacters: Boolean = true,
        override val promptOnRemoval: Boolean = true,
        override val customPasswordConfigurations: List<CustomPasswordConfiguration> = emptyList(),
    ) : ReadableConfiguration.Password
    data class CustomPasswordConfiguration(
        override val name: String = "",
        override val length: Int = DEFAULT_PASSWORD_LENGTH,
        override val hasNumbers: Boolean = true,
        override val hasLowercaseLetters: Boolean = true,
        override val hasUppercaseLetters: Boolean = true,
        override val hasSpecialCharacters: Boolean = true,
        override val unusedSpecialCharacters: String = "",
    ) : ReadableConfiguration.CustomPasswordConfiguration
    data class Adapter(
        override val clipboard: Clipboard = Clipboard(),
        override val keyStore: KeyStore = KeyStore(),
        override val passwordTree: PasswordTree = PasswordTree(),
        override val userInterface: UserInterface = UserInterface(),
    ) : ReadableConfiguration.Adapter
    data class Clipboard(override val reset: ClipboardReset = ClipboardReset()) : ReadableConfiguration.Clipboard
    data class ClipboardReset(
        override val enabled: Boolean = true,
        override val delaySeconds: Int = DEFAULT_CLIPBOARD_RESET_DELAY_SECONDS,
    ) : ReadableConfiguration.ClipboardReset
    data class PasswordTree(
        override var location: String = "",
        override val verifySignature: Boolean = true,
        override val verifyChecksum: Boolean = true,
    ) : ReadableConfiguration.PasswordTree
    data class KeyStore(override var location: String = "") : ReadableConfiguration.KeyStore
    data class UserInterface(
        override val ansiEscapeCodes: AnsiEscapeCodes = AnsiEscapeCodes(),
        override val audibleBell: Boolean = false,
        override val secureInput: Boolean = true,
    ) : ReadableConfiguration.UserInterface
    data class AnsiEscapeCodes(override val enabled: Boolean = false) : ReadableConfiguration.AnsiEscapeCodes
    data class Domain(
        override val eggIdMemory: EggIdMemory = EggIdMemory(),
        override val protein: Protein = Protein(),
    ) : ReadableConfiguration.Domain
    data class EggIdMemory(
        override val enabled: Boolean = true,
        override val persisted: Boolean = false,
    ) : ReadableConfiguration.EggIdMemory
    data class Protein(
        override val secureProteinStructureInput: Boolean = true,
        override val promptForProteinStructureInputToggle: Boolean = false,
    ) : ReadableConfiguration.Protein
}
