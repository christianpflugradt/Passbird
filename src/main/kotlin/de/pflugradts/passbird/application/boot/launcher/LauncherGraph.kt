package de.pflugradts.passbird.application.boot.launcher

import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.adapter.userinterface.TerminalInputGateway
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.keystore.KeyStoreFormatDetector
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.process.migration.PreLaunchMigrationDetector
import de.pflugradts.passbird.application.process.migration.PreLaunchMigrationLocator
import de.pflugradts.passbird.application.process.migration.keystore.KeyStoreFormatMigrationDetector
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigrationDetector
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeYolkMigrationDetector
import de.pflugradts.passbird.application.util.SystemOperation

class LauncherGraph(val runContext: RunContext) {
    val bootable: Bootable get() = passbirdLauncher
    val configuration: ReadableConfiguration by lazy { configurationFactory.loadConfiguration() }
    val preLaunchMigrationDetectors: Set<PreLaunchMigrationDetector> by lazy {
        setOf(
            KeyStoreFormatMigrationDetector(configuration, KeyStoreFormatDetector(), systemOperation),
            PasswordTreeKeyDerivationMigrationDetector(configuration, passwordTreeEnvelope, systemOperation),
            PasswordTreeYolkMigrationDetector(configuration, systemOperation),
        )
    }
    val userInterfaceAdapterPort: UserInterfaceAdapterPort by lazy {
        CommandLineInterfaceService(TerminalInputGateway(), configuration)
    }

    private val systemOperation by lazy { SystemOperation() }
    private val configurationFactory by lazy { ConfigurationFactory(systemOperation, runContext) }
    private val passwordTreeEnvelope by lazy { PasswordTreeEnvelope() }
    private val preLaunchMigrationLocator by lazy { PreLaunchMigrationLocator(preLaunchMigrationDetectors) }
    private val passbirdLauncher by lazy {
        PassbirdLauncher(
            configuration = configuration,
            preLaunchMigrationLocator = preLaunchMigrationLocator,
            runContext = runContext,
            userInterfaceAdapterPort = userInterfaceAdapterPort,
            systemOperation = systemOperation,
        )
    }
}
