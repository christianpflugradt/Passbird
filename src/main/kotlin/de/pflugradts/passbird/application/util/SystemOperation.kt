package de.pflugradts.passbird.application.util

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.util.Arrays
import kotlin.system.exitProcess

private const val JCEKS_KEYSTORE = "JCEKS"

class SystemOperation {
    val isConsoleAvailable: Boolean get() = System.console() != null
    val jceksInstance: KeyStore get() = KeyStore.getInstance(JCEKS_KEYSTORE)

    fun readPasswordFromConsole(): CharArray = System.console().readPassword()
    fun resolvePath(directory: String, fileName: String): Path? =
        tryCatching { Paths.get(directory).resolve(fileName) }.getOrNull()
    fun getPath(vararg uri: String): Path =
        if (uri.size > 1) Paths.get(uri[0], *Arrays.copyOfRange(uri, 1, uri.size)) else Paths.get(uri[0])
    fun newInputStream(path: Path): InputStream = Files.newInputStream(path)
    fun newOutputStream(path: Path): OutputStream = Files.newOutputStream(path)
    fun writeBytesToFile(path: Path, shell: Shell): Path = Files.write(path, shell.toByteArray())
    fun readBytesFromFile(path: Path) = tryCatching { shellOf(Files.readAllBytes(path)) } getOrElse emptyShell()
    fun copyToClipboard(text: String) = StringSelection(text).let { Toolkit.getDefaultToolkit().systemClipboard.setContents(it, it) }
    fun exit() { exitProcess(0) }
}
