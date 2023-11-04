package de.pflugradts.passbird.application.failure

import de.pflugradts.passbird.domain.model.password.InvalidKeyException
import kotlin.io.path.name

fun reportFailure(checksumFailure: ChecksumFailure) {
    err("Checksum of password database could not be verified.")
    if (checksumFailure.critical) {
        err(
            "Shutting down due to checksum failure. If you still think this is a valid PwMan3 password database file, " +
                "you can set the verifyChecksum option in your configuration to false.",
        )
    }
}
fun reportFailure(clipboardFailure: ClipboardFailure) =
    err("Clipboard could not be updated. Please check your Java version. Exception: ${clipboardFailure.ex.message}")
fun reportFailure(commandFailure: CommandFailure) = err("Command execution failed: ${commandFailure.ex.message}")
fun reportFailure(decryptPasswordDatabaseFailure: DecryptPasswordDatabaseFailure) =
    err("Password database could not be decrypted. Please delete the file '${decryptPasswordDatabaseFailure.path.name}' and reboot PwMan3.")
fun reportFailure(exportFailure: ExportFailure) = err("Password database could not be exported: ${exportFailure.ex.message}")
fun reportFailure(importFailure: ImportFailure) =
    if (importFailure.ex is InvalidKeyException) {
        err(
            "Password database could not be imported because at least one alias contains digits or special characters. " +
                "Please correct invalid aliases and try again. Erroneous alias: ${importFailure.ex.keyBytes.asString()}",
        )
    } else {
        err("Password database could not be imported.")
    }
fun reportFailure(inputFailure: InputFailure) = err("Input could not be processed: ${inputFailure.ex.message}")
fun reportFailure(passwordEntriesFailure: PasswordEntriesFailure) = err("Password Entries could not be accessed.")
fun reportFailure(passwordEntryFailure: PasswordEntryFailure) =
    if (passwordEntryFailure.ex is InvalidKeyException) {
        err("Password alias cannot contain digits or special characters. Please choose a different alias.")
    } else {
        err("Password entry could not be accessed.")
    }
fun reportFailure(renamePasswordEntryFailure: RenamePasswordEntryFailure) =
    err("Password alias could not be renamed: ${renamePasswordEntryFailure.ex.message}")
fun reportFailure(signatureCheckFailure: SignatureCheckFailure) {
    err("Signature of password database could not be verified.")
    if (signatureCheckFailure.critical) {
        err(
            "Shutting down due to signature failure. If you still think this is a valid PwMan3 password " +
                "database file, you can set the verifySignature option in your configuration to false.",
        )
    }
}
fun reportFailure(writePasswordDatabaseFailure: WritePasswordDatabaseFailure) =
    err("Password database could not be synced: ${writePasswordDatabaseFailure.ex.message}")

private fun err(message: String) = System.err.println(message)
