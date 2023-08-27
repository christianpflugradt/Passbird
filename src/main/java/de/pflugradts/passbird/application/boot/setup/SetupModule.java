package de.pflugradts.passbird.application.boot.setup;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import de.pflugradts.passbird.adapter.keystore.KeyStoreService;
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService;
import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.configuration.ConfigurationFactory;
import de.pflugradts.passbird.application.configuration.ConfigurationSync;
import de.pflugradts.passbird.application.configuration.ConfigurationSyncService;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.configuration.UpdatableConfiguration;

public class SetupModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Bootable.class).to(PassbirdSetup.class);
        bind(KeyStoreAdapterPort.class).to(KeyStoreService.class);
        bind(UserInterfaceAdapterPort.class).to(CommandLineInterfaceService.class);
        bind(ConfigurationSync.class).to(ConfigurationSyncService.class);
        bind(UpdatableConfiguration.class).toProvider(ConfigurationDependencyProvider.class).in(Singleton.class);
        bind(ReadableConfiguration.class).to(UpdatableConfiguration.class);
    }

    private static class ConfigurationDependencyProvider implements Provider<UpdatableConfiguration> {

        @Inject
        private ConfigurationFactory configurationFactory;

        @Override
        public UpdatableConfiguration get() {
            return configurationFactory.loadConfiguration();
        }

    }

}
