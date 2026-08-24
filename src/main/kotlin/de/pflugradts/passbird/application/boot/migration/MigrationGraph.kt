package de.pflugradts.passbird.application.boot.migration

import de.pflugradts.passbird.adapter.keystore.JceksKeyStoreService
import de.pflugradts.passbird.adapter.keystore.KeyStoreFactory
import de.pflugradts.passbird.adapter.keystore.KeyStoreService
import de.pflugradts.passbird.adapter.keystore.MigrationKeyStoreService
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.adapter.userinterface.TerminalInputGateway
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.keystore.KeyStoreFormatDetector
import de.pflugradts.passbird.application.passwordtree.LegacyCurrentPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.LegacyPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.process.migration.AuthenticatedMigrationDetector
import de.pflugradts.passbird.application.process.migration.AuthenticatedMigrationLocator
import de.pflugradts.passbird.application.process.migration.Migration
import de.pflugradts.passbird.application.process.migration.MigrationAuthenticationService
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.MigrationRunner
import de.pflugradts.passbird.application.process.migration.keystore.KeyStoreFormatMigration
import de.pflugradts.passbird.application.process.migration.keystore.KeyStoreFormatMigrationService
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeFavoritesMigration
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeFavoritesMigrationService
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigration
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigrationService
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeYolkMigration
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeYolkMigrationService
import de.pflugradts.passbird.application.util.SystemOperation

class MigrationGraph(
    val runContext: RunContext,
    val migrationRequest: MigrationRequest = MigrationRequest.empty(),
) {
    val bootable: Bootable get() = passbirdMigration
    val authenticatedMigrationDetectors: Set<AuthenticatedMigrationDetector> = emptySet()
    val configuration: ReadableConfiguration by lazy { configurationFactory.loadConfiguration() }
    val keyStoreAdapterPort: KeyStoreAdapterPort by lazy {
        MigrationKeyStoreService(keyStoreService, jceksKeyStoreService, KeyStoreFormatDetector(), systemOperation)
    }
    val migrations: Set<Migration> by lazy {
        setOf(
            KeyStoreFormatMigration(keyStoreFormatMigrationService, migrationAuthenticationService, systemOperation),
            PasswordTreeKeyDerivationMigration(migrationAuthenticationService, passwordTreeKeyDerivationMigrationService, systemOperation),
            PasswordTreeFavoritesMigration(migrationAuthenticationService, passwordTreeFavoritesMigrationService, systemOperation),
            PasswordTreeYolkMigration(migrationAuthenticationService, passwordTreeYolkMigrationService, systemOperation),
        )
    }
    val userInterfaceAdapterPort: UserInterfaceAdapterPort by lazy {
        CommandLineInterfaceService(TerminalInputGateway(), configuration)
    }

    private val systemOperation by lazy { SystemOperation() }
    private val configurationFactory by lazy { ConfigurationFactory(systemOperation, runContext) }
    private val keyStoreFactory by lazy { KeyStoreFactory() }
    private val keyStoreService by lazy { KeyStoreService(systemOperation, keyStoreFactory) }
    private val jceksKeyStoreService by lazy { JceksKeyStoreService(systemOperation, keyStoreFactory) }
    private val passwordTreeEnvelope by lazy { PasswordTreeEnvelope() }
    private val legacyPasswordTreePayloadReader by lazy { LegacyPasswordTreePayloadReader(configuration, systemOperation) }
    private val legacyCurrentPasswordTreePayloadReader by lazy {
        LegacyCurrentPasswordTreePayloadReader(configuration, systemOperation)
    }
    private val passwordTreePayloadWriter by lazy { PasswordTreePayloadWriter() }
    private val authenticatedMigrationLocator by lazy { AuthenticatedMigrationLocator(authenticatedMigrationDetectors) }
    private val migrationAuthenticationService by lazy {
        MigrationAuthenticationService(configuration, keyStoreAdapterPort, userInterfaceAdapterPort, systemOperation)
    }
    private val migrationRunner by lazy { MigrationRunner(migrations) }
    private val keyStoreFormatMigrationService by lazy {
        KeyStoreFormatMigrationService(configuration, keyStoreAdapterPort, systemOperation)
    }
    private val passwordTreeKeyDerivationMigrationService by lazy {
        PasswordTreeKeyDerivationMigrationService(
            configuration = configuration,
            passwordTreeEnvelope = passwordTreeEnvelope,
            legacyPasswordTreePayloadReader = legacyPasswordTreePayloadReader,
            passwordTreePayloadWriter = passwordTreePayloadWriter,
            systemOperation = systemOperation,
        )
    }
    private val passwordTreeFavoritesMigrationService by lazy {
        PasswordTreeFavoritesMigrationService(
            configuration = configuration,
            legacyPasswordTreePayloadReader = legacyPasswordTreePayloadReader,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadWriter = passwordTreePayloadWriter,
            systemOperation = systemOperation,
        )
    }
    private val passwordTreeYolkMigrationService by lazy {
        PasswordTreeYolkMigrationService(
            configuration = configuration,
            legacyCurrentPasswordTreePayloadReader = legacyCurrentPasswordTreePayloadReader,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadWriter = passwordTreePayloadWriter,
            systemOperation = systemOperation,
        )
    }
    private val passbirdMigration by lazy {
        PassbirdMigration(
            authenticatedMigrationLocator = authenticatedMigrationLocator,
            migrationAuthenticationService = migrationAuthenticationService,
            migrationRequest = migrationRequest,
            migrationRunner = migrationRunner,
            userInterfaceAdapterPort = userInterfaceAdapterPort,
            systemOperation = systemOperation,
        )
    }
}
