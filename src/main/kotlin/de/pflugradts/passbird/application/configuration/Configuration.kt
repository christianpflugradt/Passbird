package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.domain.model.egg.PasswordRequirements

private const val DEFAULT_CLIPBOARD_RESET_DELAY_SECONDS = 10
private const val DEFAULT_PASSWORD_LENGTH = 20

data class Configuration(
    @Transient override var template: Boolean = false,
    override val application: Application = Application(),
    override val adapter: Adapter = Adapter(),
) : UpdatableConfiguration {

    override fun parsePasswordRequirements() = PasswordRequirements(
        length = application.password.length,
        hasSpecialCharacters = application.password.specialCharacters,
    )

    override fun updateDirectory(directory: Directory) {
        adapter.keyStore.location = directory.value
        adapter.passwordStore.location = directory.value
    }

    data class Application(
        override val inactivityLimit: InactivityLimit = InactivityLimit(),
        override val password: Password = Password(),
    ) : ReadableConfiguration.Application
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
        override val passwordStore: PasswordStore = PasswordStore(),
        override val userInterface: UserInterface = UserInterface(),
    ) : ReadableConfiguration.Adapter
    data class Clipboard(override val reset: ClipboardReset = ClipboardReset()) : ReadableConfiguration.Clipboard
    data class ClipboardReset(
        override val enabled: Boolean = true,
        override val delaySeconds: Int = DEFAULT_CLIPBOARD_RESET_DELAY_SECONDS,
    ) : ReadableConfiguration.ClipboardReset
    data class PasswordStore(
        override var location: String = "",
        override val verifySignature: Boolean = true,
        override val verifyChecksum: Boolean = true,
    ) : ReadableConfiguration.PasswordStore
    data class KeyStore(override var location: String = "") : ReadableConfiguration.KeyStore
    data class UserInterface(
        override val ansiEscapeCodes: AnsiEscapeCodes = AnsiEscapeCodes(),
        override val audibleBell: Boolean = false,
        override val secureInput: Boolean = true,
    ) : ReadableConfiguration.UserInterface
    data class AnsiEscapeCodes(override val enabled: Boolean = false) : ReadableConfiguration.AnsiEscapeCodes
}
