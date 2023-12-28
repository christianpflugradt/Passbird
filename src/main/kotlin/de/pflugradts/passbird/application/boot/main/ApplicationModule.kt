package de.pflugradts.passbird.application.boot.main

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.multibindings.Multibinder
import de.pflugradts.passbird.adapter.clipboard.ClipboardService
import de.pflugradts.passbird.adapter.exchange.FilePasswordExchange
import de.pflugradts.passbird.adapter.keystore.KeyStoreService
import de.pflugradts.passbird.adapter.passwordstore.PasswordStoreFacade
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.CustomSetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.DiscardCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.GetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.RenameCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.SetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ViewCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.AddNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.AssignNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.DiscardNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.SwitchNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.ViewNestCommandHandler
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.eventhandling.ApplicationEventHandler
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.exchange.ExchangeFactory
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.exchange.PasswordImportExportService
import de.pflugradts.passbird.application.security.CryptoProviderFactory
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.NestingGroundService
import de.pflugradts.passbird.domain.service.eventhandling.DomainEventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordFacade
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.provider.RandomPasswordProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import de.pflugradts.passbird.domain.service.password.storage.NestingGround
import de.pflugradts.passbird.domain.service.password.storage.PasswordStoreAdapterPort

class ApplicationModule : AbstractModule() {
    override fun configure() {
        configureApplication()
        configureMultibinders()
        configureProviders()
    }

    private fun configureApplication() {
        bind(Bootable::class.java).to(PassbirdApplication::class.java)
        bind(ClipboardAdapterPort::class.java).to(ClipboardService::class.java)
        bind(EventRegistry::class.java).to(PassbirdEventRegistry::class.java)
        bind(ImportExportService::class.java).to(PasswordImportExportService::class.java)
        bind(KeyStoreAdapterPort::class.java).to(KeyStoreService::class.java)
        bind(EggRepository::class.java).to(NestingGround::class.java)
        bind(NestService::class.java).to(NestingGroundService::class.java)
        bind(PasswordProvider::class.java).to(RandomPasswordProvider::class.java)
        bind(PasswordService::class.java).to(PasswordFacade::class.java)
        bind(PasswordStoreAdapterPort::class.java).to(PasswordStoreFacade::class.java)
        bind(UserInterfaceAdapterPort::class.java).to(CommandLineInterfaceService::class.java)
    }

    private fun configureMultibinders() {
        val commandHandlerMultibinder = Multibinder.newSetBinder(binder(), CommandHandler::class.java)
        listOf(
            AddNestCommandHandler::class.java,
            AssignNestCommandHandler::class.java,
            CustomSetCommandHandler::class.java,
            DiscardCommandHandler::class.java,
            DiscardNestCommandHandler::class.java,
            ExportCommandHandler::class.java,
            GetCommandHandler::class.java,
            HelpCommandHandler::class.java,
            ImportCommandHandler::class.java,
            ListCommandHandler::class.java,
            QuitCommandHandler::class.java,
            RenameCommandHandler::class.java,
            SetCommandHandler::class.java,
            SwitchNestCommandHandler::class.java,
            ViewCommandHandler::class.java,
            ViewNestCommandHandler::class.java,
        ).forEach { commandHandlerMultibinder.addBinding().to(it) }
        val eventHandlerMultibinder = Multibinder.newSetBinder(binder(), EventHandler::class.java)
        listOf(
            ApplicationEventHandler::class.java,
            DomainEventHandler::class.java,
        ).forEach { eventHandlerMultibinder.addBinding().to(it) }
    }

    private fun configureProviders() {
        bind(ReadableConfiguration::class.java).toProvider(ConfigurationDependencyProvider::class.java).`in`(Singleton::class.java)
        bind(CryptoProvider::class.java).toProvider(CryptoProviderDependencyProvider::class.java).`in`(Singleton::class.java)
        install(
            FactoryModuleBuilder().implement(ExchangeAdapterPort::class.java, FilePasswordExchange::class.java)
                .build(ExchangeFactory::class.java),
        )
    }

    private class CryptoProviderDependencyProvider @Inject constructor(
        @Inject private val cryptoProviderFactory: CryptoProviderFactory,
    ) : Provider<CryptoProvider> {
        override fun get() = cryptoProviderFactory.createCryptoProvider()
    }

    private class ConfigurationDependencyProvider @Inject constructor(
        @Inject private val configurationFactory: ConfigurationFactory,
    ) : Provider<ReadableConfiguration> {
        override fun get(): ReadableConfiguration = configurationFactory.loadConfiguration()
    }
}
