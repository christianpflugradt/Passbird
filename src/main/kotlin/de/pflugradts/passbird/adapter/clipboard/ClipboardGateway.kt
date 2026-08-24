package de.pflugradts.passbird.adapter.clipboard

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8

class ClipboardGateway constructor(
    private val nativeClipboardGateway: NativeClipboardGateway = NativeClipboardGateway(),
    private val awtClipboardGateway: AwtClipboardGateway = AwtClipboardGateway(),
) {
    fun copy(text: String, nativeToolingEnabled: Boolean) {
        if (nativeToolingEnabled) {
            runCatching { nativeClipboardGateway.copy(text) }.onSuccess { return }
        }
        awtClipboardGateway.copy(text)
    }
}

class NativeClipboardGateway constructor(
    private val clipboardCommandLauncher: ClipboardCommandLauncher = ClipboardCommandLauncher(),
    private val environmentGateway: EnvironmentGateway = EnvironmentGateway(),
    private val operatingSystemGateway: OperatingSystemGateway = OperatingSystemGateway(),
) {
    fun copy(text: String) {
        val commands = commandsFor(operatingSystemGateway.current())
        var lastFailure: Exception? = null
        for (command in commands) {
            val result = runCatching { clipboardCommandLauncher.copy(command, text) }
            if (result.isSuccess) {
                return
            }
            lastFailure = result.exceptionOrNull() as Exception
        }
        throw lastFailure ?: IllegalStateException("No native clipboard utility available")
    }

    private fun commandsFor(operatingSystem: OperatingSystem): List<List<String>> = when (operatingSystem) {
        OperatingSystem.MAC_OS -> listOf(listOf("pbcopy"))

        OperatingSystem.WINDOWS -> listOf(listOf("cmd", "/c", "clip"))

        OperatingSystem.LINUX -> buildList {
            if (!environmentGateway.value("WAYLAND_DISPLAY").isNullOrBlank()) {
                add(listOf("wl-copy"))
            }
            if (!environmentGateway.value("DISPLAY").isNullOrBlank()) {
                add(listOf("xclip", "-selection", "clipboard"))
                add(listOf("xsel", "--clipboard", "--input"))
            }
            if (isEmpty()) {
                add(listOf("wl-copy"))
                add(listOf("xclip", "-selection", "clipboard"))
                add(listOf("xsel", "--clipboard", "--input"))
            }
        }

        OperatingSystem.UNKNOWN -> emptyList()
    }
}

class ClipboardCommandLauncher constructor() {
    fun copy(command: List<String>, text: String) {
        val process = ProcessBuilder(command).start()
        process.outputStream.use { it.write(text.toByteArray(UTF_8)) }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader(UTF_8).use { it.readText().trim() }
            throw IOException(error.ifBlank { "Clipboard command exited with status $exitCode" })
        }
    }
}

class AwtClipboardGateway constructor() {
    fun copy(text: String) = StringSelection(text).let { Toolkit.getDefaultToolkit().systemClipboard.setContents(it, it) }
}

class EnvironmentGateway constructor() {
    fun value(name: String): String? = System.getenv(name)
}

class OperatingSystemGateway constructor() {
    fun current(): OperatingSystem {
        val operatingSystem = System.getProperty("os.name").lowercase()
        return when {
            operatingSystem.contains("windows") -> OperatingSystem.WINDOWS
            operatingSystem.contains("mac") || operatingSystem.contains("darwin") -> OperatingSystem.MAC_OS
            operatingSystem.contains("linux") -> OperatingSystem.LINUX
            else -> OperatingSystem.UNKNOWN
        }
    }
}

enum class OperatingSystem {
    LINUX,
    MAC_OS,
    WINDOWS,
    UNKNOWN,
}
