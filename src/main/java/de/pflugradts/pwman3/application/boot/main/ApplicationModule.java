package de.pflugradts.pwman3.application.boot.main;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import de.pflugradts.pwman3.adapter.clipboard.ClipboardService;
import de.pflugradts.pwman3.adapter.exchange.FilePasswordExchange;
import de.pflugradts.pwman3.adapter.keystore.KeyStoreService;
import de.pflugradts.pwman3.adapter.passwordstore.PasswordFileStore;
import de.pflugradts.pwman3.adapter.userinterface.CommandLineInterfaceService;
import de.pflugradts.pwman3.application.ClipboardAdapterPort;
import de.pflugradts.pwman3.application.ExchangeAdapterPort;
import de.pflugradts.pwman3.application.KeyStoreAdapterPort;
import de.pflugradts.pwman3.application.PasswordStoreAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.commandhandling.handler.CommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.CustomSetCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.DiscardCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.ExportCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.GetCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.HelpCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.ImportCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.ListCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.QuitCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.SetCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.ViewCommandHandler;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.configuration.ConfigurationFactory;
import de.pflugradts.pwman3.application.exchange.ExchangeFactory;
import de.pflugradts.pwman3.application.exchange.ImportExportService;
import de.pflugradts.pwman3.application.exchange.PasswordImportExportService;
import de.pflugradts.pwman3.application.security.CryptoProvider;
import de.pflugradts.pwman3.application.security.CryptoProviderFactory;
import de.pflugradts.pwman3.domain.service.PasswordProvider;
import de.pflugradts.pwman3.domain.service.PasswordRepositoryService;
import de.pflugradts.pwman3.domain.service.PasswordService;
import de.pflugradts.pwman3.domain.service.RandomPasswordProvider;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.ExcessiveImports"})
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        configureApplication();
        configureMultibinders();
        configureProviders();
    }

    private void configureApplication() {
        bind(Bootable.class).to(PwMan3Application.class);
        bind(ClipboardAdapterPort.class).to(ClipboardService.class);
        bind(ImportExportService.class).to(PasswordImportExportService.class);
        bind(KeyStoreAdapterPort.class).to(KeyStoreService.class);
        bind(PasswordProvider.class).to(RandomPasswordProvider.class);
        bind(PasswordService.class).to(PasswordRepositoryService.class);
        bind(PasswordStoreAdapterPort.class).to(PasswordFileStore.class);
        bind(UserInterfaceAdapterPort.class).to(CommandLineInterfaceService.class);
    }

    private void configureMultibinders() {
        final Multibinder<CommandHandler> commandHandlerMultibinder =
                Multibinder.newSetBinder(binder(), CommandHandler.class);
        commandHandlerMultibinder.addBinding().to(CustomSetCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(DiscardCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ExportCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(GetCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(HelpCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ImportCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ListCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(QuitCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(SetCommandHandler.class);
        commandHandlerMultibinder.addBinding().to(ViewCommandHandler.class);
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

