package de.pflugradts.passbird.application.failure

import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.nio.file.Path

interface Failure

data class ChecksumFailure(val actualChecksum: Byte, val expectedChecksum: Byte, val critical: Boolean) : Failure
data class ClipboardFailure(val ex: Exception) : Failure
data class CommandFailure(val ex: Exception) : Failure
data class DecryptPasswordDatabaseFailure(val path: Path, val ex: Exception) : Failure
data class ExportFailure(val ex: Exception) : Failure
data class ImportFailure(val ex: Exception) : Failure
data class EggsFailure(val ex: Exception) : Failure
data class EggFailure(val bytes: Bytes, val ex: Exception) : Failure
data class RenameEggFailure(val ex: Exception) : Failure
data class SignatureCheckFailure(val actualSignature: Bytes, val critical: Boolean) : Failure
data class WritePasswordDatabaseFailure(val path: Path, val ex: Exception) : Failure
