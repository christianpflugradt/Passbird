package de.pflugradts.passbird.application.boot.migration

import com.google.inject.AbstractModule
import com.google.inject.Provider
import com.google.inject.multibindings.Multibinder
import de.pflugradts.passbird.adapter.keystore.MigrationKeyStoreService
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.process.migration.AuthenticatedMigrationDetector
import de.pflugradts.passbird.application.process.migration.Migration
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.keystore.KeyStoreFormatMigration
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigration
import jakarta.inject.Inject
import jakarta.inject.Singleton

class MigrationModule(
    private val runContext: RunContext,
    private val migrationRequest: MigrationRequest = MigrationRequest.empty(),
) : AbstractModule() {
    override fun configure() {
        bind(RunContext::class.java).toInstance(runContext)
        bind(MigrationRequest::class.java).toInstance(migrationRequest)
        bind(Bootable::class.java).to(PassbirdMigration::class.java)
        bind(KeyStoreAdapterPort::class.java).to(MigrationKeyStoreService::class.java)
        bind(ReadableConfiguration::class.java).toProvider(ConfigurationDependencyProvider::class.java).`in`(Singleton::class.java)
        bind(UserInterfaceAdapterPort::class.java).to(CommandLineInterfaceService::class.java)
        configureMultibinders()
    }

    private fun configureMultibinders() {
        Multibinder.newSetBinder(binder(), AuthenticatedMigrationDetector::class.java)
        Multibinder.newSetBinder(binder(), Migration::class.java).apply {
            addBinding().to(KeyStoreFormatMigration::class.java)
            addBinding().to(PasswordTreeKeyDerivationMigration::class.java)
        }
    }

    private class ConfigurationDependencyProvider @Inject constructor(
        private val configurationFactory: ConfigurationFactory,
    ) : Provider<ReadableConfiguration> {
        override fun get(): ReadableConfiguration = configurationFactory.loadConfiguration()
    }
}
