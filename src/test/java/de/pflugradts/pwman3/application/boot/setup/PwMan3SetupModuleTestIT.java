package de.pflugradts.pwman3.application.boot.setup;

import com.google.inject.Guice;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.KeyStoreAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.configuration.ConfigurationSync;
import de.pflugradts.pwman3.application.configuration.UpdatableConfiguration;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PwMan3SetupModuleTestIT {

    @Test
    void shouldResolveAllDependencies() {
        // given / when
        final var actual = Guice.createInjector(new SetupModule())
                .getInstance(PwMan3TestSetup.class);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.getBootable()).isNotNull().isInstanceOf(PwMan3Setup.class);
        assertThat(actual.getKeyStoreAdapterPort()).isNotNull();
        assertThat(actual.getUserInterfaceAdapterPort()).isNotNull();
        assertThat(actual.getConfigurationSync()).isNotNull();
        assertThat(actual.getConfiguration()).isNotNull();
        assertThat(actual.getUpdatableConfiguration()).isNotNull();
        assertThat(actual.getUpdatableConfiguration()).isSameAs(actual.getConfiguration());
    }

    @Getter
    private static class PwMan3TestSetup {

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
