package de.pflugradts.passbird.application.boot.setup
import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationSync
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import java.nio.file.Paths
class PassbirdSetup constructor(
    private val setupGuide: SetupGuide,
    private val configurationSync: ConfigurationSync,
    private val configuration: ReadableConfiguration,
    private val keyStoreAdapterPort: KeyStoreAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val systemOperation: SystemOperation,
) : Bootable {
    override fun boot() {
        setupGuide.sendWelcome()
        try {
            if (configuration.template) {
                setupGuide.sendConfigTemplateRouteInformation()
                if (continueRoute()) {
                    configTemplateRoute()
                }
            } else {
                setupGuide.sendConfigKeyStoreRouteInformation(configuration.adapter.keyStore.location)
                if (continueRoute()) {
                    configKeyStoreRoute()
                }
            }
        } catch (_: SecureInputUnavailableException) {
        }
        setupGuide.sendGoodbye()
        systemOperation.exit()
    }
    private fun continueRoute() = userInterfaceAdapterPort.receiveConfirmation(outputOf(shellOf("Your input: ")))
    private fun configTemplateRoute() {
        setupGuide.sendInputPath("configuration")
        val directory = receiveValidDirectory()
        if (createConfiguration(directory).failure) {
            return
        }
        setupGuide.sendCreateKeyStoreInformation()
        createKeyStore(directory, receiveMasterPassword())
        setupGuide.sendRestart()
    }
    private fun configKeyStoreRoute() {
        setupGuide.sendInputPath("keystore")
        setupGuide.sendCreateKeyStoreInformation()
        createKeyStore(receiveValidDirectory(), receiveMasterPassword())
        setupGuide.sendRestart()
    }
    private fun createConfiguration(directory: Directory) = configurationSync.sync(directory)
    private fun receiveMasterPassword(): PlainShell {
        var input: Input? = null
        var inputRepeated: Input? = null
        while (true) {
            try {
                input = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("first input: ")))
                inputRepeated = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("second input: ")))
            } catch (ex: SecureInputUnavailableException) {
                input?.invalidate()
                inputRepeated?.invalidate()
                throw ex
            }
            if (input.isNotEmpty && input == inputRepeated) {
                val password = input.toPlainShell()
                inputRepeated.invalidate()
                userInterfaceAdapterPort.sendLineBreak()
                return password
            }
            input.invalidate()
            inputRepeated.invalidate()
            setupGuide.sendNonMatchingInputs()
        }
    }
    private fun createKeyStore(directory: Directory, password: PlainShell) {
        keyStoreAdapterPort.storeKey(
            password,
            Paths.get(directory.value).resolve(ReadableConfiguration.KEYSTORE_FILENAME),
        )
        setupGuide.sendCreateKeyStoreSucceeded()
    }
    private fun receiveValidDirectory(): Directory {
        var directory = Directory(userInterfaceAdapterPort.receive(outputOf(shellOf("your input: "))).shell.asString())
        while (!isValidDirectory(directory)) {
            directory = Directory(userInterfaceAdapterPort.receive(outputOf(shellOf("your input: "))).shell.asString())
        }
        return directory
    }
    private fun isValidDirectory(directory: Directory) = systemOperation.exists(directory) && systemOperation.isDirectory(directory)
}
