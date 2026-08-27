package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.yolk.normalizeTotpAlgorithm
import de.pflugradts.passbird.domain.model.egg.PasswordRequirements

private const val DEFAULT_BACKUP_DIRECTORY = "backups"
private const val DEFAULT_CLIPBOARD_RESET_DELAY_SECONDS = 10
private const val DEFAULT_CLIPBOARD_NATIVE_TOOLING_ENABLED = true
private const val DEFAULT_PASSWORD_LENGTH = 20
private const val DEFAULT_TRASH_RETENTION_DAYS = 365
private const val DEFAULT_YOLK_ALGORITHM = "SHA1"
private const val DEFAULT_YOLK_COPY_TO_CLIPBOARD = true
private const val DEFAULT_YOLK_DIGITS = 6
private const val DEFAULT_YOLK_PERIOD_SECONDS = 30

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

    override fun updateKeyStoreDirectory(directory: Directory) {
        adapter.keyStore.location = directory.value
    }

    data class Application(
        override val backup: Backup = Backup(),
        override val exchange: Exchange = Exchange(),
        override val inactivityLimit: InactivityLimit = InactivityLimit(),
        override val password: Password = Password(),
        override val trash: Trash = Trash(),
        override val yolk: Yolk = Yolk(),
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
    data class Trash(
        override val retentionDays: Int = DEFAULT_TRASH_RETENTION_DAYS,
    ) : ReadableConfiguration.Trash
    data class Yolk(
        override val algorithm: String = DEFAULT_YOLK_ALGORITHM,
        override val copyToClipboard: Boolean = DEFAULT_YOLK_COPY_TO_CLIPBOARD,
        override val digits: Int = DEFAULT_YOLK_DIGITS,
        override val periodSeconds: Int = DEFAULT_YOLK_PERIOD_SECONDS,
    ) : ReadableConfiguration.Yolk
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
    data class Clipboard(
        override val nativeTooling: ClipboardNativeTooling = ClipboardNativeTooling(),
        override val reset: ClipboardReset = ClipboardReset(),
    ) : ReadableConfiguration.Clipboard
    data class ClipboardNativeTooling(
        override val enabled: Boolean = DEFAULT_CLIPBOARD_NATIVE_TOOLING_ENABLED,
    ) : ReadableConfiguration.ClipboardNativeTooling
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
        override val updateOnFavoriteUse: Boolean = true,
    ) : ReadableConfiguration.EggIdMemory
    data class Protein(
        override val secureProteinStructureInput: Boolean = true,
        override val promptForProteinStructureInputToggle: Boolean = false,
        override val templates: List<ProteinTemplate> = emptyList(),
    ) : ReadableConfiguration.Protein

    data class ProteinTemplate(
        override val name: String = "",
        private val definedSlots: MutableMap<Int, String> = linkedMapOf(),
    ) : ReadableConfiguration.ProteinTemplate {
        fun putSlot(key: String, value: String) {
            definedSlots[key.toIntOrNull() ?: Int.MIN_VALUE] = value
        }

        override val slots: Map<Int, String> get() = definedSlots.toMap()
    }
}

internal fun Configuration.validate(): Configuration {
    normalizeTotpAlgorithm(application.yolk.algorithm)
    require(application.yolk.digits == 6 || application.yolk.digits == 8) { "Yolk digits are invalid" }
    require(application.yolk.periodSeconds > 0) { "Yolk period is invalid" }
    require(application.trash.retentionDays >= 0) { "Trash retention is invalid" }
    val names = domain.protein.templates.map { it.name }
    require(names.none { it.isBlank() }) { "Protein template name is invalid" }
    require(names.distinct().size == names.size) { "Protein template names must be unique" }
    domain.protein.templates.forEach { template ->
        require(template.slots.keys.all { it in 0..9 }) { "Protein template '${template.name}' contains invalid slot" }
    }
    return this
}
