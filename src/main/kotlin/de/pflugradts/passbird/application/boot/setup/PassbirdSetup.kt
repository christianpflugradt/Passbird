package de.pflugradts.passbird.application.boot.setup

import com.google.inject.Inject
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationSync
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
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
        terminate(systemOperation)
    }

    private fun continueRoute() = userInterfaceAdapterPort.receiveConfirmation(outputOf(bytesOf("Your input: ")))

    private fun configTemplateRoute() {
        setupGuide.sendInputPath("configuration")
        createConfiguration(verifyValidDirectory(configuration.adapter.passwordStore.location))
        setupGuide.sendCreateKeyStoreInformation()
        createKeyStore(configuration.adapter.keyStore.location, receiveMasterPassword())
        setupGuide.sendRestart()
    }

    private fun configKeyStoreRoute() {
        setupGuide.sendInputPath("keystore")
        setupGuide.sendCreateKeyStoreInformation()
        createKeyStore(verifyValidDirectory(configuration.adapter.keyStore.location), receiveMasterPassword())
        setupGuide.sendRestart()
    }

    private fun createConfiguration(directory: String) = configurationSync.sync(directory)

    private fun receiveMasterPassword(): Input {
        var input = emptyInput()
        var inputRepeated = emptyInput()
        while (input.isEmpty || input != inputRepeated) {
            setupGuide.sendNonMatchingInputs()
            input = userInterfaceAdapterPort.receiveSecurely(outputOf(bytesOf("first input: ")))
            inputRepeated = userInterfaceAdapterPort.receiveSecurely(outputOf(bytesOf("second input: ")))
        }
        userInterfaceAdapterPort.sendLineBreak()
        return input
    }

    private fun createKeyStore(directory: String, password: Input) {
        keyStoreAdapterPort.storeKey(
            password.bytes.toChars(),
            Paths.get(directory).resolve(ReadableConfiguration.KEYSTORE_FILENAME),
        )
        setupGuide.sendCreateKeyStoreSucceeded()
    }

    private fun verifyValidDirectory(source: String): String {
        var directory = source
        while (!isValidDirectory(directory)) {
            directory = userInterfaceAdapterPort.receive(outputOf(bytesOf("your input: "))).bytes.asString()
        }
        return directory
    }

    private fun isValidDirectory(directory: String) =
        systemOperation.getPath(directory).toFile().let { it.isDirectory && it.exists() }
}
