package de.pflugradts.pwman3.application.boot.launcher;

import com.google.inject.Guice;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PwMan3LauncherModuleTestIT {

    @Test
    void shouldResolveAllDependencies() {
        // given / when
        final var actual = Guice.createInjector(new LauncherModule())
                .getInstance(PwMan3TestLauncher.class);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.getBootable()).isNotNull().isInstanceOf(PwMan3Launcher.class);
        assertThat(actual.getConfiguration()).isNotNull();
        assertThat(actual.getUserInterfaceAdapterPort()).isNotNull();
    }

    @Getter
    private static class PwMan3TestLauncher {

        @Inject
        private Bootable bootable;
        @Inject
        private ReadableConfiguration configuration;
        @Inject
        private UserInterfaceAdapterPort userInterfaceAdapterPort;

    }

}
