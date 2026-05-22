package de.pflugradts.passbird.application.boot.setup

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import de.pflugradts.passbird.adapter.keystore.KeyStoreService
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ConfigurationSync
import de.pflugradts.passbird.application.configuration.ConfigurationSyncService
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.UpdatableConfiguration

class SetupModule : AbstractModule() {
    override fun configure() {
        bind(Bootable::class.java).to(PassbirdSetup::class.java)
        bind(KeyStoreAdapterPort::class.java).to(KeyStoreService::class.java)
        bind(UserInterfaceAdapterPort::class.java).to(CommandLineInterfaceService::class.java)
        bind(ConfigurationSync::class.java).to(ConfigurationSyncService::class.java)
        bind(UpdatableConfiguration::class.java).toProvider(ConfigurationDependencyProvider::class.java).`in`(Singleton::class.java)
        bind(ReadableConfiguration::class.java).to(UpdatableConfiguration::class.java)
    }

    private class ConfigurationDependencyProvider @Inject constructor(
        @Inject val configurationFactory: ConfigurationFactory,
    ) : Provider<UpdatableConfiguration> {
        override fun get(): UpdatableConfiguration = configurationFactory.loadConfiguration()
    }
}
