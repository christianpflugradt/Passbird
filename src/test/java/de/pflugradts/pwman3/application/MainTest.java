package de.pflugradts.pwman3.application;

import com.google.inject.Injector;
import com.google.inject.Module;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.boot.launcher.PwMan3Launcher;
import de.pflugradts.pwman3.application.util.GuiceInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MainTest {

    @Mock
    private GuiceInjector guiceInjector;
    @InjectMocks
    private Main main;

    @Captor
    private ArgumentCaptor<Module> captor;

    @BeforeEach
    private void setup() {
        MockitoAnnotations.initMocks(this);
        System.clearProperty(CONFIGURATION_SYSTEM_PROPERTY);
    }

    @Test
    void shouldSetSystemPropertyAndBootLauncher() {
        // given
        final var configurationDirectory = "tmp";
        final var expectedBootable = mock(PwMan3Launcher.class);
        setupInjectorWithCapture(expectedBootable);

        // when
        main.boot(configurationDirectory);

        // then
        then(expectedBootable).should().boot();
        assertThat(System.getProperty(CONFIGURATION_SYSTEM_PROPERTY)).isNotNull()
                .isEqualTo(configurationDirectory);
    }

    @Test
    void shouldNotSetSystemPropertyIfArgsIsNotProvided() {
        // given
        setupInjector();

        // when
        main.boot();

        // then
        assertThat(System.getProperty(CONFIGURATION_SYSTEM_PROPERTY)).isNullOrEmpty();
    }

    @Test
    void shouldNotSetSystemPropertyIfArgsHasSizeUnequalTo1() {
        // given
        setupInjector();

        // when
        main.boot("tmp", "/dev/null");

        // then
        assertThat(System.getProperty(CONFIGURATION_SYSTEM_PROPERTY)).isNullOrEmpty();
    }

    private void setupInjector() {
        final var injector = mock(Injector.class);
        given(injector.getInstance(Bootable.class)).willReturn(mock(Bootable.class));
        given(guiceInjector.create(any())).willReturn(injector);
    }

    private void setupInjectorWithCapture(final Bootable bootable) {
        final var injector = mock(Injector.class);
        given(injector.getInstance(Bootable.class)).willReturn(bootable);
        given(guiceInjector.create(captor.capture())).willReturn(injector);
    }

}
