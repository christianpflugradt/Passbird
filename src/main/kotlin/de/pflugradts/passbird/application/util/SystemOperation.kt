package de.pflugradts.passbird.application.util

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.FileName
import de.pflugradts.passbird.application.toFileName
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.time.Clock
import kotlin.io.path.name
import kotlin.system.exitProcess

const val FAILURE_EXIT_STATUS = 1
const val SUCCESS_EXIT_STATUS = 0
private val PRIVATE_DIRECTORY_PERMISSIONS = setOf(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE)
private val PRIVATE_FILE_PERMISSIONS = setOf(OWNER_READ, OWNER_WRITE)

class SystemOperation {
    val clock = Clock.systemUTC()

    fun getFileNames(directory: Directory): List<FileName> = Files.list(getPath(directory)).map { it.name.toFileName() }.toList()
    fun getPath(directory: Directory): Path = Paths.get(directory.value)
    fun resolvePath(directory: Directory, fileName: FileName): Path = getPath(directory).resolve(fileName.value)
    fun resolvePath(directory: Directory, other: Directory): Path = getPath(directory).resolve(other.value)
    fun copyTo(source: Path, target: Path) {
        writeToSensitiveFile(target) { outputStream ->
            newInputStream(source).use { inputStream -> inputStream.copyTo(outputStream) }
        }
    }
    fun createDirectory(directory: Directory) {
        Files.createDirectories(getPath(directory))
        applyPosixPermissionsIfSupported(getPath(directory), PRIVATE_DIRECTORY_PERMISSIONS)
    }
    fun delete(path: Path) = Files.delete(path)
    fun exists(directory: Directory): Boolean = exists(getPath(directory))
    fun exists(path: Path): Boolean = Files.exists(path)
    fun isDirectory(directory: Directory): Boolean = Files.isDirectory(getPath(directory))
    fun newInputStream(path: Path): InputStream = Files.newInputStream(path)
    fun newOutputStream(path: Path): OutputStream = Files.newOutputStream(path)
    fun writeToSensitiveFile(path: Path, write: (OutputStream) -> Unit): Path {
        val targetPath = path.toAbsolutePath()
        val tempPath = Files.createTempFile(targetPath.parent, ".${targetPath.fileName}.", ".tmp")
        return tryCatching {
            newOutputStream(tempPath).use(write)
            Files.move(tempPath, targetPath, REPLACE_EXISTING)
            applyPosixPermissionsIfSupported(targetPath, PRIVATE_FILE_PERMISSIONS)
            targetPath
        }.let { result ->
            result.exceptionOrNull()?.let {
                runCatching { Files.deleteIfExists(tempPath) }
                throw it
            }
            result.getOrNull()!!
        }
    }
    fun writeBytesToSensitiveFile(path: Path, byteArray: ByteArray): Path = writeToSensitiveFile(path) { outputStream ->
        outputStream.write(byteArray)
    }
    fun writeStringToSensitiveFile(path: Path, content: String): Path = writeToSensitiveFile(path) { outputStream ->
        outputStream.write(content.toByteArray())
    }
    fun readBytesFromFile(path: Path): ByteArray = Files.readAllBytes(path)
    fun sleep(milliseconds: Long) = Thread.sleep(milliseconds)
    fun exit(status: Int = SUCCESS_EXIT_STATUS): Unit = exitProcess(status)

    private fun applyPosixPermissionsIfSupported(path: Path, permissions: Set<java.nio.file.attribute.PosixFilePermission>) {
        runCatching {
            Files.getFileAttributeView(path, PosixFileAttributeView::class.java)?.setPermissions(permissions)
        }
    }
}
