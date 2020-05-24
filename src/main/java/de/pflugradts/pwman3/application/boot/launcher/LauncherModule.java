package de.pflugradts.pwman3.application.boot.launcher;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.adapter.userinterface.CommandLineInterfaceService;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.configuration.ConfigurationFactory;

public class LauncherModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Bootable.class).to(PwMan3Launcher.class);
        bind(ReadableConfiguration.class).toProvider(ConfigurationDependencyProvider.class).in(Singleton.class);
        bind(UserInterfaceAdapterPort.class).to(CommandLineInterfaceService.class);
    }

    static class ConfigurationDependencyProvider implements Provider<ReadableConfiguration> {

        @Inject
        private ConfigurationFactory configurationFactory;

        @Override
        public ReadableConfiguration get() {
            return configurationFactory.loadConfiguration();
        }

    }

}

