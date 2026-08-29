package de.pflugradts.passbird.application.process.hotkey

import de.pflugradts.passbird.application.GlobalHotkeyAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

class GlobalHotkeyStartupChecker(
    private val configuration: ReadableConfiguration,
    private val globalHotkeyAdapterPort: GlobalHotkeyAdapterPort,
    private val systemOperation: SystemOperation,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : Initializer {
    override fun run() {
        if (!configuration.application.flow.globalHotkey.enabled) {
            return
        }
        if (globalHotkeyAdapterPort.prepareOnStartup()) {
            return
        }
        userInterfaceAdapterPort.sendLineBreak()
        userInterfaceAdapterPort.send(
            outputOf(
                shellOf(
                    "Global hotkey permission is required on macOS. " +
                        "Allow Input Monitoring for the terminal app that starts Passbird, " +
                        "restart that terminal app, and then start Passbird again.",
                ),
            ),
        )
        userInterfaceAdapterPort.sendLineBreak()
        systemOperation.exit(FAILURE_EXIT_STATUS)
    }
}
