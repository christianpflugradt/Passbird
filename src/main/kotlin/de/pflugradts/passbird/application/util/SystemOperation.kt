package de.pflugradts.passbird.application.util

import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.util.Arrays
import kotlin.system.exitProcess

/**
 * Wraps interactions with the operating system.
 */
class SystemOperation {
    val isConsoleAvailable: Boolean get() = System.console() != null
    val jceksInstance: KeyStore get() = KeyStore.getInstance(CryptoUtils.JCEKS_KEYSTORE)
    val desktop: Desktop get() = Desktop.getDesktop()

    fun readPasswordFromConsole(): CharArray = System.console().readPassword()
    fun resolvePath(directory: String, fileName: String): Path? =
        try { Paths.get(directory).resolve(fileName) } catch (ex: InvalidPathException) { null }
    fun getPath(vararg uri: String): Path =
        if (uri.size > 1) Paths.get(uri[0], *Arrays.copyOfRange(uri, 1, uri.size)) else Paths.get(uri[0])
    fun newInputStream(path: Path): InputStream = Files.newInputStream(path)
    fun newOutputStream(path: Path): OutputStream = Files.newOutputStream(path)
    fun writeBytesToFile(path: Path, bytes: Bytes): Path = Files.write(path, bytes.toByteArray())
    fun readBytesFromFile(path: Path) = try { bytesOf(Files.readAllBytes(path)) } catch (ex: IOException) { emptyBytes() }
    fun copyToClipboard(text: String) = StringSelection(text).let { Toolkit.getDefaultToolkit().systemClipboard.setContents(it, it) }
    fun getResourceAsBytes(resource: String) =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)?.readAllBytes()?.let { bytesOf(it) } ?: emptyBytes()
    fun openFile(file: File) = if (file.getName().endsWith(".html")) desktop.browse(file.toURI()) else desktop.open(file)
    fun exit() { exitProcess(0) }
}
