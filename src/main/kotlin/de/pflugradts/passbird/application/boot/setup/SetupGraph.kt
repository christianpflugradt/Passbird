package de.pflugradts.passbird.application.boot.setup

import de.pflugradts.passbird.adapter.keystore.KeyStoreFactory
import de.pflugradts.passbird.adapter.keystore.KeyStoreService
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.adapter.userinterface.TerminalInputGateway
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ConfigurationSync
import de.pflugradts.passbird.application.configuration.ConfigurationSyncService
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.UpdatableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation

class SetupGraph(val runContext: RunContext) {
    val bootable: Bootable get() = passbirdSetup
    val configuration: ReadableConfiguration get() = updatableConfiguration
    val configurationSync: ConfigurationSync by lazy { ConfigurationSyncService(updatableConfiguration, systemOperation) }
    val keyStoreAdapterPort: KeyStoreAdapterPort by lazy { KeyStoreService(systemOperation, KeyStoreFactory()) }
    val userInterfaceAdapterPort: UserInterfaceAdapterPort by lazy {
        CommandLineInterfaceService(TerminalInputGateway(), configuration)
    }

    private val systemOperation by lazy { SystemOperation() }
    private val configurationFactory by lazy { ConfigurationFactory(systemOperation, runContext) }
    private val updatableConfiguration: UpdatableConfiguration by lazy { configurationFactory.loadConfiguration() }
    private val setupGuide by lazy { SetupGuide(userInterfaceAdapterPort) }
    private val passbirdSetup by lazy {
        PassbirdSetup(
            setupGuide = setupGuide,
            configurationSync = configurationSync,
            configurationDirectory = runContext.homeDirectory,
            configuration = configuration,
            keyStoreAdapterPort = keyStoreAdapterPort,
            userInterfaceAdapterPort = userInterfaceAdapterPort,
            systemOperation = systemOperation,
        )
    }
}
