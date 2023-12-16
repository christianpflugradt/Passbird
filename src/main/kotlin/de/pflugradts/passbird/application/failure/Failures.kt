package de.pflugradts.passbird.application.failure

import de.pflugradts.passbird.domain.model.shell.Shell
import java.nio.file.Path

interface Failure

data class ChecksumFailure(val actualChecksum: Byte, val expectedChecksum: Byte, val critical: Boolean) : Failure
data class ClipboardFailure(val ex: Exception) : Failure
data class CommandFailure(val ex: Exception) : Failure
data class ConfigurationFailure(val ex: Exception) : Failure
data class DecryptPasswordDatabaseFailure(val path: Path, val ex: Exception) : Failure
data class ExportFailure(val ex: Exception) : Failure
data class ImportFailure(val ex: Exception) : Failure
data class EggsFailure(val ex: Exception) : Failure
data class EggFailure(val shell: Shell, val ex: Exception) : Failure
data class RenameEggFailure(val ex: Exception) : Failure
data class SignatureCheckFailure(val actualSignature: Shell, val critical: Boolean) : Failure
data class WritePasswordDatabaseFailure(val path: Path, val ex: Exception) : Failure
