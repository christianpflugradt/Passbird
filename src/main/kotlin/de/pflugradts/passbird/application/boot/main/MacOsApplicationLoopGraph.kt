package de.pflugradts.passbird.application.boot.main

import de.pflugradts.passbird.adapter.userinterface.hotkey.carbon.CarbonMacOsApplicationLoopRunner
import de.pflugradts.passbird.application.GlobalHotkeyBackend

internal fun interface MacOsApplicationLoopRunner {
    fun run(application: () -> Unit)
}

internal class MacOsApplicationLoopGraph(
    private val osName: String = System.getProperty("os.name").orEmpty(),
    private val globalHotkeyEnabled: Boolean = true,
    private val globalHotkeyBackend: GlobalHotkeyBackend = GlobalHotkeyBackend.AUTO,
    private val applicationLoopRunner: MacOsApplicationLoopRunner = CarbonMacOsApplicationLoopRunner(),
) {
    fun run(application: () -> Unit) {
        if (!requiresMacOsApplicationLoop()) {
            application()
            return
        }
        applicationLoopRunner.run(application)
    }

    private fun requiresMacOsApplicationLoop() = osName.lowercase().contains("mac") &&
        globalHotkeyEnabled &&
        globalHotkeyBackend.resolvePolicy(osName).requiresMacOsApplicationLoop()
}
