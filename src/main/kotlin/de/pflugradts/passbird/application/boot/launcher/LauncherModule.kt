package de.pflugradts.passbird.application.boot.launcher

import com.google.inject.AbstractModule
import com.google.inject.Provider
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import jakarta.inject.Inject
import jakarta.inject.Singleton

class LauncherModule(private val runContext: RunContext) : AbstractModule() {
    override fun configure() {
        bind(RunContext::class.java).toInstance(runContext)
        bind(Bootable::class.java).to(PassbirdLauncher::class.java)
        bind(ReadableConfiguration::class.java).toProvider(ConfigurationDependencyProvider::class.java).`in`(Singleton::class.java)
        bind(UserInterfaceAdapterPort::class.java).to(CommandLineInterfaceService::class.java)
    }

    class ConfigurationDependencyProvider @Inject constructor(
        private val configurationFactory: ConfigurationFactory,
    ) : Provider<ReadableConfiguration> {
        override fun get(): ReadableConfiguration = configurationFactory.loadConfiguration()
    }
}
