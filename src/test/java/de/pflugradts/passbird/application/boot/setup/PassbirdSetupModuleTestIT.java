package de.pflugradts.passbird.application.boot.setup;

import com.google.inject.Guice;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.configuration.ConfigurationSync;
import de.pflugradts.passbird.application.configuration.UpdatableConfiguration;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PassbirdSetupModuleTestIT {

    @Test
    void shouldResolveAllDependencies() {
        // given / when
        final var actual = Guice.createInjector(new SetupModule())
                .getInstance(PassbirdTestSetup.class);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.getBootable()).isNotNull().isInstanceOf(PassbirdSetup.class);
        assertThat(actual.getKeyStoreAdapterPort()).isNotNull();
        assertThat(actual.getUserInterfaceAdapterPort()).isNotNull();
        assertThat(actual.getConfigurationSync()).isNotNull();
        assertThat(actual.getConfiguration()).isNotNull();
        assertThat(actual.getUpdatableConfiguration()).isNotNull();
        assertThat(actual.getUpdatableConfiguration()).isSameAs(actual.getConfiguration());
    }

    @Getter
    private static class PassbirdTestSetup {

        @Inject
        private Bootable bootable;
        @Inject
        private KeyStoreAdapterPort keyStoreAdapterPort;
        @Inject
        private UserInterfaceAdapterPort userInterfaceAdapterPort;
        @Inject
        private ConfigurationSync configurationSync;
        @Inject
        private ReadableConfiguration configuration;
        @Inject
        private UpdatableConfiguration updatableConfiguration;

    }

}
