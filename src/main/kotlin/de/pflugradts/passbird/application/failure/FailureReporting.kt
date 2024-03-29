package de.pflugradts.passbird.application.failure

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.DOES_NOT_EXIST
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.IS_NOT_A_DIRECTORY
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.IS_NULL
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import kotlin.io.path.name

fun reportFailure(checksumFailure: ChecksumFailure) {
    err("Checksum of Password Tree could not be verified.")
    if (checksumFailure.critical) {
        err(
            "Shutting down due to checksum failure. If you still think this is a valid Passbird Tree, " +
                "you can set the verifyChecksum option in your configuration to false.",
        )
    }
}
fun reportFailure(clipboardFailure: ClipboardFailure) =
    err("Clipboard could not be updated. Please check your Java version. Exception: ${clipboardFailure.ex.message}")
fun reportFailure(commandFailure: CommandFailure) = err("Command execution failed: ${commandFailure.ex.message}")
fun reportFailure(configurationFailure: ConfigurationFailure) = if (configurationFailure.ex is UnrecognizedPropertyException) {
    err(
        "Configuration contains unrecognized property and will not be used. Please remove the unrecognized field " +
            "before restarting Passbird: ${configurationFailure.ex.message?.split("(")?.get(0)}",
    )
} else {
    err("Configuration could not be loaded: ${configurationFailure.ex.message}")
}
fun reportFailure(passwordTreeFailure: DecryptPasswordTreeFailure) =
    err("Password Tree at '${passwordTreeFailure.path.name}' could not be decrypted: ${passwordTreeFailure.ex.message}")
fun reportFailure(exportFailure: ExportFailure) = err("Password Tree could not be exported: ${exportFailure.ex.message}")
fun reportFailure(homeDirectoryFailure: HomeDirectoryFailure) = err(
    "Shutting down: " + when (homeDirectoryFailure.case) {
        IS_NULL -> "No home directory was given upon starting Passbird."
        DOES_NOT_EXIST -> "Specified home directory does not exist: ${homeDirectoryFailure.homeDirectory}"
        IS_NOT_A_DIRECTORY -> "Specified home directory is actually not a directory: ${homeDirectoryFailure.homeDirectory}"
    },
)
fun reportFailure(importFailure: ImportFailure) = if (importFailure.ex is InvalidEggIdException) {
    err(
        "Password Tree could not be imported because at least one eggId contains digits or special characters. " +
            "Please correct invalid eggIds and try again. Erroneous eggId: ${importFailure.ex.eggIdShell.asString()}",
    )
} else {
    err("Password Tree could not be imported.")
}
fun reportFailure(loginFailure: LoginFailure) = err("Login failed. Shutting down after ${loginFailure.attempts} unsuccessful attempts.")
fun reportFailure(signatureCheckFailure: SignatureCheckFailure) {
    err("Signature of Password Tree could not be verified.")
    if (signatureCheckFailure.critical) {
        err(
            "Shutting down due to signature failure. If you still think this is a valid Passbird Tree," +
                "you can set the verifySignature option in your configuration to false.",
        )
    }
}
fun reportFailure(writePasswordTreeFailure: WritePasswordTreeFailure) =
    err("Password Tree could not be synced: ${writePasswordTreeFailure.ex.message}")

private fun err(message: String) = System.err.println(message)
