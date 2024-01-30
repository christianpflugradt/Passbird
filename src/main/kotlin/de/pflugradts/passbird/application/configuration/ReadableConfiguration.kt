package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.domain.model.egg.PasswordRequirements

interface ReadableConfiguration {
    fun parsePasswordRequirements(): PasswordRequirements
    val template: Boolean
    val application: Application
    val adapter: Adapter

    interface Application { val password: Password }
    interface Password {
        val length: Int
        val specialCharacters: Boolean
        val promptOnRemoval: Boolean
    }
    interface Adapter {
        val clipboard: Clipboard
        val keyStore: KeyStore
        val passwordStore: PasswordStore
        val userInterface: UserInterface
    }
    interface Clipboard { val reset: ClipboardReset }
    interface ClipboardReset {
        val enabled: Boolean
        val delaySeconds: Int
    }
    interface PasswordStore {
        val location: String
        val verifySignature: Boolean
        val verifyChecksum: Boolean
    }
    interface KeyStore { val location: String }
    interface UserInterface {
        val ansiEscapeCodes: AnsiEscapeCodes
        val audibleBell: Boolean
        val secureInput: Boolean
    }
    interface AnsiEscapeCodes { val enabled: Boolean }

    companion object {
        const val CONFIGURATION_FILENAME = "passbird.yml"
        const val KEYSTORE_FILENAME = "passbird.ks"
        const val DATABASE_FILENAME = "passbird.pw"
        const val EXCHANGE_FILENAME = "passbird-export.json"
    }
}
