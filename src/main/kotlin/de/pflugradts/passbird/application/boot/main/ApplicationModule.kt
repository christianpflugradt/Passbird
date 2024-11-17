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
import de.pflugradts.passbird.adapter.passwordtree.PasswordTreeFacade
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.SetInfoCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.CustomSetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.DiscardCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.GetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.RenameCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.SetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.ViewCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.AddNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.DiscardNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.MoveToNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.SwitchNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.ViewNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.DiscardProteinCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.GetProteinCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.ProteinInfoCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.SetProteinCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.ViewProteinStructuresCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.ViewProteinTypesCommandHandler
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.eventhandling.ApplicationEventHandler
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.exchange.ExchangeFactory
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.exchange.PasswordImportExportService
import de.pflugradts.passbird.application.process.Finalizer
import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.application.process.backup.BackupManager
import de.pflugradts.passbird.application.process.exchange.ExportFileChecker
import de.pflugradts.passbird.application.process.inactivity.InactivityHandlerScheduler
import de.pflugradts.passbird.application.security.CryptoProviderFactory
import de.pflugradts.passbird.domain.service.eventhandling.DomainEventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.nest.NestingGroundService
import de.pflugradts.passbird.domain.service.password.PasswordFacade
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.provider.RandomPasswordProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import de.pflugradts.passbird.domain.service.password.tree.NestingGround
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort

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
        bind(PasswordTreeAdapterPort::class.java).to(PasswordTreeFacade::class.java)
        bind(UserInterfaceAdapterPort::class.java).to(CommandLineInterfaceService::class.java)
    }

    private fun configureMultibinders() {
        Multibinder.newSetBinder(binder(), CommandHandler::class.java).apply {
            listOf(
                AddNestCommandHandler::class.java,
                MoveToNestCommandHandler::class.java,
                CustomSetCommandHandler::class.java,
                DiscardCommandHandler::class.java,
                DiscardNestCommandHandler::class.java,
                DiscardProteinCommandHandler::class.java,
                ExportCommandHandler::class.java,
                GetCommandHandler::class.java,
                GetProteinCommandHandler::class.java,
                HelpCommandHandler::class.java,
                ImportCommandHandler::class.java,
                ListCommandHandler::class.java,
                ProteinInfoCommandHandler::class.java,
                QuitCommandHandler::class.java,
                RenameCommandHandler::class.java,
                SetCommandHandler::class.java,
                SetInfoCommandHandler::class.java,
                SetProteinCommandHandler::class.java,
                SwitchNestCommandHandler::class.java,
                ViewCommandHandler::class.java,
                ViewNestCommandHandler::class.java,
                ViewProteinStructuresCommandHandler::class.java,
                ViewProteinTypesCommandHandler::class.java,
            ).forEach { this.addBinding().to(it) }
        }
        Multibinder.newSetBinder(binder(), EventHandler::class.java).apply {
            listOf(
                ApplicationEventHandler::class.java,
                DomainEventHandler::class.java,
            ).forEach { this.addBinding().to(it) }
        }
        Multibinder.newSetBinder(binder(), Initializer::class.java).apply {
            listOf(
                ExportFileChecker::class.java,
                InactivityHandlerScheduler::class.java,
            ).forEach { this.addBinding().to(it) }
        }
        Multibinder.newSetBinder(binder(), Finalizer::class.java).apply {
            listOf(
                BackupManager::class.java,
            ).forEach { this.addBinding().to(it) }
        }
    }

    private fun configureProviders() {
        bind(ReadableConfiguration::class.java).toProvider(ConfigurationDependencyProvider::class.java).`in`(Singleton::class.java)
        bind(CryptoProvider::class.java).toProvider(CryptoProviderDependencyProvider::class.java).`in`(Singleton::class.java)
        install(
            FactoryModuleBuilder().implement(ExchangeAdapterPort::class.java, FilePasswordExchange::class.java)
                .build(ExchangeFactory::class.java),
        )
    }

    private class ConfigurationDependencyProvider @Inject constructor(
        private val configurationFactory: ConfigurationFactory,
    ) : Provider<ReadableConfiguration> {
        override fun get(): ReadableConfiguration = configurationFactory.loadConfiguration()
    }

    private class CryptoProviderDependencyProvider @Inject constructor(
        private val cryptoProviderFactory: CryptoProviderFactory,
    ) : Provider<CryptoProvider> {
        override fun get() = cryptoProviderFactory.createCryptoProvider()
    }
}
