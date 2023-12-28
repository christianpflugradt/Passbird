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
data class HomeDirectoryFailure(val homeDirectory: String? = null, val case: HomeDirectoryFailureCase) : Failure
data class ImportFailure(val ex: Exception) : Failure
data class LoginFailure(val attempts: Int) : Failure
data class SignatureCheckFailure(val actualSignature: Shell, val critical: Boolean) : Failure
data class WritePasswordDatabaseFailure(val path: Path, val ex: Exception) : Failure

enum class HomeDirectoryFailureCase { IS_NULL, DOES_NOT_EXIST, IS_NOT_A_DIRECTORY }
