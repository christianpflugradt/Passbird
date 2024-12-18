package de.pflugradts.passbird.application.util

import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.FileName
import de.pflugradts.passbird.application.toFileName
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.time.Clock
import kotlin.io.path.name
import kotlin.system.exitProcess

private const val JCEKS_KEYSTORE = "JCEKS"

class SystemOperation {
    val clock = Clock.systemUTC()
    val isConsoleAvailable: Boolean get() = System.console() != null
    val jceksInstance: KeyStore get() = KeyStore.getInstance(JCEKS_KEYSTORE)

    fun readPasswordFromConsole(): CharArray = System.console().readPassword()
    fun getFileNames(directory: Directory): List<FileName> = Files.list(getPath(directory)).map { it.name.toFileName() }.toList()
    fun getPath(directory: Directory): Path = Paths.get(directory.value)
    fun resolvePath(directory: Directory, fileName: FileName): Path = getPath(directory).resolve(fileName.value)
    fun resolvePath(directory: Directory, other: Directory): Path = getPath(directory).resolve(other.value)
    fun copyTo(source: Path, target: Path) {
        Files.copy(source, target)
    }
    fun createDirectory(directory: Directory) {
        Files.createDirectories(getPath(directory))
    }
    fun delete(path: Path) = Files.delete(path)
    fun exists(directory: Directory): Boolean = exists(getPath(directory))
    fun exists(path: Path): Boolean = Files.exists(path)
    fun isDirectory(directory: Directory): Boolean = Files.isDirectory(getPath(directory))
    fun newInputStream(path: Path): InputStream = Files.newInputStream(path)
    fun newOutputStream(path: Path): OutputStream = Files.newOutputStream(path)
    fun writeBytesToFile(path: Path, byteArray: ByteArray): Path = Files.write(path, byteArray)
    fun readBytesFromFile(path: Path): ByteArray = Files.readAllBytes(path)
    fun copyToClipboard(text: String) = StringSelection(text).let { Toolkit.getDefaultToolkit().systemClipboard.setContents(it, it) }
    fun exit(): Unit = exitProcess(0)
}
