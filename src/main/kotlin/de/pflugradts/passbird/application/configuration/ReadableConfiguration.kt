package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.domain.model.password.PasswordRequirements

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
    interface UserInterface { val secureInput: Boolean }

    companion object {
        const val CONFIGURATION_SYSTEM_PROPERTY = "config"
        const val CONFIGURATION_FILENAME = "PwMan3.yml"
        const val KEYSTORE_FILENAME = "PwMan3.ks"
        const val DATABASE_FILENAME = "PwMan3.pw"
        const val EXCHANGE_FILENAME = "PwMan3.json"
    }
}