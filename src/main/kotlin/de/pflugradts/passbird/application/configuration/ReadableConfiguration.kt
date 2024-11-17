package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.egg.Protein

interface ReadableConfiguration {
    fun parsePasswordRequirements(): PasswordRequirements
    val template: Boolean
    val application: Application
    val adapter: Adapter
    val domain: Domain

    interface Application {
        val backup: Backup
        val exchange: Exchange
        val inactivityLimit: InactivityLimit
        val password: Password
    }
    interface Backup {
        val location: String
        val numberOfBackups: Int
        val configuration: BackupSettings
        val keyStore: BackupSettings
        val passwordTree: BackupSettings
    }
    interface BackupSettings {
        val enabled: Boolean
        val location: String?
        val numberOfBackups: Int?
    }
    interface Exchange {
        val promptOnExportFile: Boolean
    }
    interface InactivityLimit {
        val enabled: Boolean
        val limitInMinutes: Int
    }
    interface Password {
        val length: Int
        val specialCharacters: Boolean
        val promptOnRemoval: Boolean
        val customPasswordConfigurations: List<CustomPasswordConfiguration>
    }
    interface CustomPasswordConfiguration {
        val name: String
        val length: Int
        val hasNumbers: Boolean
        val hasLowercaseLetters: Boolean
        val hasUppercaseLetters: Boolean
        val hasSpecialCharacters: Boolean
        val unusedSpecialCharacters: String
    }
    interface Adapter {
        val clipboard: Clipboard
        val keyStore: KeyStore
        val passwordTree: PasswordTree
        val userInterface: UserInterface
    }
    interface Clipboard {
        val reset: ClipboardReset
    }
    interface ClipboardReset {
        val enabled: Boolean
        val delaySeconds: Int
    }
    interface PasswordTree {
        val location: String
        val verifySignature: Boolean
        val verifyChecksum: Boolean
    }
    interface KeyStore {
        val location: String
    }
    interface UserInterface {
        val ansiEscapeCodes: AnsiEscapeCodes
        val audibleBell: Boolean
        val secureInput: Boolean
    }
    interface AnsiEscapeCodes {
        val enabled: Boolean
    }

    interface Domain {
        val protein: Protein
    }

    interface Protein {
        val secureProteinStructureInput: Boolean
        val promptForProteinStructureInputToggle: Boolean
    }

    companion object {
        const val CONFIGURATION_FILENAME = "passbird.yml"
        const val KEYSTORE_FILENAME = "passbird.sec"
        const val PASSWORD_TREE_FILENAME = "passbird.tree"
        const val EXCHANGE_FILENAME = "passbird-export.json"
    }
}
