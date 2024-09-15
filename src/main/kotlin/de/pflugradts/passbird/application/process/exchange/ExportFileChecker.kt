package de.pflugradts.passbird.application.process.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.application.Global
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

class ExportFileChecker @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val systemOperation: SystemOperation,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : Initializer {
    override fun run() {
        val exchangeFile = systemOperation.resolvePath(Global.homeDirectory, ReadableConfiguration.EXCHANGE_FILENAME.toFileName())
        if (configuration.application.exchange.promptOnExportFile && exchangeFile.toFile().exists()) {
            val prompt = "An password export file has been detected. Should this file be deleted? Y/n "
            if (userInterfaceAdapterPort.receiveYes(outputOf(shellOf(prompt)))) {
                systemOperation.delete(exchangeFile)
                userInterfaceAdapterPort.send(outputOf(shellOf("Export file has been successfully deleted.")))
            } else {
                val msg = "Export file has not been deleted but prompt for deletion will be repeated upon next program start."
                userInterfaceAdapterPort.send(outputOf(shellOf(msg)))
            }
            userInterfaceAdapterPort.sendLineBreak()
        }
    }
}
