package de.pflugradts.passbird.application

enum class GlobalHotkeyBackend(val configurationValue: String) {
    AUTO("auto"),
    WIN32("win32"),
    CARBON("carbon"),
    QUARTZ("quartz"),
    X11("x11"),
    ;

    fun resolvePolicy(osName: String, display: String? = null) = when (this) {
        AUTO -> when {
            osName.isWindows() -> GlobalHotkeyBackendPolicy(GlobalHotkeyRegistrarBackend.WIN32)
            osName.isMacOs() -> GlobalHotkeyBackendPolicy(GlobalHotkeyRegistrarBackend.CARBON)
            display.hasX11Display() -> GlobalHotkeyBackendPolicy(GlobalHotkeyRegistrarBackend.X11)
            else -> GlobalHotkeyBackendPolicy.Unsupported
        }

        WIN32 -> supportedPolicy(osName.isWindows(), GlobalHotkeyRegistrarBackend.WIN32)

        CARBON -> supportedPolicy(osName.isMacOs(), GlobalHotkeyRegistrarBackend.CARBON)

        QUARTZ -> supportedPolicy(osName.isMacOs(), GlobalHotkeyRegistrarBackend.QUARTZ, preparesOnStartup = true)

        X11 -> supportedPolicy(display.hasX11Display(), GlobalHotkeyRegistrarBackend.X11)
    }

    companion object {
        fun fromConfiguration(value: String) = entries.firstOrNull { it.configurationValue == value }

        fun supportedConfigurationValues() = entries.map(GlobalHotkeyBackend::configurationValue).toSet()
    }
}

class GlobalHotkeyBackendPolicy(
    val registrarBackend: GlobalHotkeyRegistrarBackend?,
    val preparesOnStartup: Boolean = false,
) {
    fun requiresMacOsApplicationLoop() = registrarBackend == GlobalHotkeyRegistrarBackend.CARBON

    val isSupported get() = registrarBackend != null

    companion object {
        val Unsupported = GlobalHotkeyBackendPolicy(registrarBackend = null)
    }
}

enum class GlobalHotkeyRegistrarBackend {
    WIN32,
    CARBON,
    QUARTZ,
    X11,
}

private fun supportedPolicy(isCompatible: Boolean, registrarBackend: GlobalHotkeyRegistrarBackend, preparesOnStartup: Boolean = false) =
    if (isCompatible) {
        GlobalHotkeyBackendPolicy(registrarBackend, preparesOnStartup)
    } else {
        GlobalHotkeyBackendPolicy.Unsupported
    }

private fun String.isWindows() = lowercase().contains("win")

private fun String.isMacOs() = lowercase().contains("mac")

private fun String?.hasX11Display() = !isNullOrBlank()
