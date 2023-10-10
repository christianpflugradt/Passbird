package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.domain.model.password.PasswordRequirements

private const val DEFAULT_CLIPBOARD_RESET_DELAY_SECONDS = 10
private const val DEFAULT_PASSWORD_LENGTH = 20

data class Configuration(
    @Transient override var template: Boolean = false,
    override val application: Application = Application(),
    override val adapter: Adapter = Adapter(),
) : UpdatableConfiguration {

    override fun parsePasswordRequirements(): PasswordRequirements =
        PasswordRequirements.of(application.password.specialCharacters, application.password.length)

    override fun updateDirectory(directory: String) {
        adapter.keyStore.location = directory
        adapter.passwordStore.location = directory
    }

    data class Application(override val password: Password = Password()) : ReadableConfiguration.Application
    data class Password(
        override val length: Int = DEFAULT_PASSWORD_LENGTH,
        override val specialCharacters: Boolean = true,
        override val promptOnRemoval: Boolean = true,
    ) : ReadableConfiguration.Password
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
    data class UserInterface(override val secureInput: Boolean = true) : ReadableConfiguration.UserInterface
}
