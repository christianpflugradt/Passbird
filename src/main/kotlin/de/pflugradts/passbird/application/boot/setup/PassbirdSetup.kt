package de.pflugradts.passbird.application.boot.setup

import com.google.inject.Inject
import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationSync
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.emptyInput
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import java.nio.file.Paths

class PassbirdSetup @Inject constructor(
    @Inject private val setupGuide: SetupGuide,
    @Inject private val configurationSync: ConfigurationSync,
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val keyStoreAdapterPort: KeyStoreAdapterPort,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    @Inject private val systemOperation: SystemOperation,
) : Bootable {
    override fun boot() {
        setupGuide.sendWelcome()
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
        setupGuide.sendGoodbye()
        systemOperation.exit()
    }

    private fun continueRoute() = userInterfaceAdapterPort.receiveConfirmation(outputOf(shellOf("Your input: ")))

    private fun configTemplateRoute() {
        setupGuide.sendInputPath("configuration")
        createConfiguration(verifyValidDirectory(Directory(configuration.adapter.passwordStore.location)))
        setupGuide.sendCreateKeyStoreInformation()
        createKeyStore(Directory(configuration.adapter.keyStore.location), receiveMasterPassword())
        setupGuide.sendRestart()
    }

    private fun configKeyStoreRoute() {
        setupGuide.sendInputPath("keystore")
        setupGuide.sendCreateKeyStoreInformation()
        createKeyStore(verifyValidDirectory(Directory(configuration.adapter.keyStore.location)), receiveMasterPassword())
        setupGuide.sendRestart()
    }

    private fun createConfiguration(directory: Directory) = configurationSync.sync(directory)

    private fun receiveMasterPassword(): Input {
        var input = emptyInput()
        var inputRepeated = emptyInput()
        while (input.isEmpty || input != inputRepeated) {
            setupGuide.sendNonMatchingInputs()
            input = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("first input: ")))
            inputRepeated = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("second input: ")))
        }
        userInterfaceAdapterPort.sendLineBreak()
        return input
    }

    private fun createKeyStore(directory: Directory, password: Input) {
        keyStoreAdapterPort.storeKey(
            password.shell.toPlainShell(),
            Paths.get(directory.value).resolve(ReadableConfiguration.KEYSTORE_FILENAME),
        )
        setupGuide.sendCreateKeyStoreSucceeded()
    }

    private fun verifyValidDirectory(givenDirectory: Directory): Directory {
        var directory = givenDirectory
        while (!isValidDirectory(directory)) {
            directory = Directory(userInterfaceAdapterPort.receive(outputOf(shellOf("your input: "))).shell.asString())
        }
        return directory
    }

    private fun isValidDirectory(directory: Directory) = systemOperation.getPath(directory).toFile().let { it.isDirectory && it.exists() }
}
