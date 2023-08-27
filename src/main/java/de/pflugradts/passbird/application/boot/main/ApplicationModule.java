package de.pflugradts.passbird.application.boot.main;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import de.pflugradts.passbird.adapter.clipboard.ClipboardService;
import de.pflugradts.passbird.adapter.exchange.FilePasswordExchange;
import de.pflugradts.passbird.adapter.keystore.KeyStoreService;
import de.pflugradts.passbird.adapter.passwordstore.PasswordStoreFacade;
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService;
import de.pflugradts.passbird.application.ClipboardAdapterPort;
import de.pflugradts.passbird.application.ExchangeAdapterPort;
import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.CustomSetCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.DiscardCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.GetCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.RenameCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.SetCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.ViewCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AddNamespaceCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AssignNamespaceCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.SwitchNamespaceCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.ViewNamespaceCommandHandler;
import de.pflugradts.passbird.application.configuration.ConfigurationFactory;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.eventhandling.ApplicationEventHandler;
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry;
import de.pflugradts.passbird.application.exchange.ExchangeFactory;
import de.pflugradts.passbird.application.exchange.ImportExportService;
import de.pflugradts.passbird.application.exchange.PasswordImportExportService;
import de.pflugradts.passbird.application.security.CryptoProviderFactory;
import de.pflugradts.passbird.domain.service.NamespaceService;
import de.pflugradts.passbird.domain.service.eventhandling.DomainEventHandler;
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.PasswordFacade;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider;
import de.pflugradts.passbird.domain.service.password.provider.RandomPasswordProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import de.pflugradts.passbird.domain.service.password.storage.PasswordStoreAdapterPort;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.ExcessiveImports"})
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        configureApplication();
        configureMultibinders();
        configureProviders();
    }

    private void configureApplication() {
        bind(Bootable.class).to(PassbirdApplication.class);
        bind(ClipboardAdapterPort.class).to(ClipboardService.class);
        bind(EventRegistry.class).to(PassbirdEventRegistry.class);
        bind(ImportExportService.class).to(PasswordImportExportService.class);
        bind(KeyStoreAdapterPort.class).to(KeyStoreService.class);
        bind(NamespaceService.class).in(Singleton.class);
        bind(PasswordEntryRepository.class).in(Singleton.class);
        bind(PasswordProvider.class).to(RandomPasswordProvider.class);
        bind(PasswordService.class).to(PasswordFacade.class);
        bind(PasswordStoreAdapterPort.class).to(PasswordStoreFacade.class);
        bind(UserInterfaceAdapterPort.class).to(CommandLineInterfaceService.class);
    }

    private void configureMultibinders() {
        final Multibinder<CommandHandler> commandHandlerMultibinder =
                Multibinder.newSetBinder(binder(), CommandHandler.class);
        commandHandlerMultibinder.addBinding().to(AddNamespaceCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(AssignNamespaceCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(CustomSetCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(DiscardCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ExportCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(GetCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(HelpCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ImportCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ListCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(QuitCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(RenameCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(SetCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(SwitchNamespaceCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ViewCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ViewNamespaceCommandHandler.class);
        final Multibinder<EventHandler> eventHandlerMultibinder =
                Multibinder.newSetBinder(binder(), EventHandler.class);
        eventHandlerMultibinder.addBinding().to(ApplicationEventHandler.class);
        eventHandlerMultibinder.addBinding().to(DomainEventHandler.class);
    }

    private void configureProviders() {
        bind(ReadableConfiguration.class).toProvider(ConfigurationDependencyProvider.class).in(Singleton.class);
        bind(CryptoProvider.class).toProvider(CryptoProviderDependencyProvider.class).in(Singleton.class);
        install(new FactoryModuleBuilder().implement(ExchangeAdapterPort.class, FilePasswordExchange.class)
                .build(ExchangeFactory.class));
    }

    private static class CryptoProviderDependencyProvider implements Provider<CryptoProvider> {

        @Inject
        private CryptoProviderFactory cryptoProviderFactory;

        @Override
        public CryptoProvider get() {
            return cryptoProviderFactory.createCryptoProvider();
        }

    }

    private static class ConfigurationDependencyProvider implements Provider<ReadableConfiguration> {

        @Inject
        private ConfigurationFactory configurationFactory;

        @Override
        public ReadableConfiguration get() {
            return configurationFactory.loadConfiguration();
        }

    }

}

