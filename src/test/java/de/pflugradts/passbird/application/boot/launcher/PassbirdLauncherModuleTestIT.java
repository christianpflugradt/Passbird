package de.pflugradts.passbird.application.boot.launcher;

import com.google.inject.Guice;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PassbirdLauncherModuleTestIT {

    @Test
    void shouldResolveAllDependencies() {
        // given / when
        final var actual = Guice.createInjector(new LauncherModule())
                .getInstance(PassbirdTestLauncher.class);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.getBootable()).isNotNull().isInstanceOf(PassbirdLauncher.class);
        assertThat(actual.getConfiguration()).isNotNull();
        assertThat(actual.getUserInterfaceAdapterPort()).isNotNull();
    }

    @Getter
    private static class PassbirdTestLauncher {

        @Inject
        private Bootable bootable;
        @Inject
        private ReadableConfiguration configuration;
        @Inject
        private UserInterfaceAdapterPort userInterfaceAdapterPort;

    }

}
