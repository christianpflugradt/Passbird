package de.pflugradts.pwman3.application.boot.setup;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.adapter.keystore.KeyStoreService;
import de.pflugradts.pwman3.adapter.userinterface.CommandLineInterfaceService;
import de.pflugradts.pwman3.application.KeyStoreAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.configuration.ConfigurationFactory;
import de.pflugradts.pwman3.application.configuration.ConfigurationSync;
import de.pflugradts.pwman3.application.configuration.ConfigurationSyncService;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.configuration.UpdatableConfiguration;

public class SetupModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Bootable.class).to(PwMan3Setup.class);
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
