package de.pflugradts.pwman3.application.boot.launcher;

import com.google.inject.Injector;
import com.google.inject.Module;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.boot.main.ApplicationModule;
import de.pflugradts.pwman3.application.boot.main.PwMan3Application;
import de.pflugradts.pwman3.application.boot.setup.PwMan3Setup;
import de.pflugradts.pwman3.application.boot.setup.SetupModule;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.application.util.FileFaker;
import de.pflugradts.pwman3.application.util.GuiceInjector;
import de.pflugradts.pwman3.application.util.PathFaker;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.application.util.SystemOperationFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PwMan3LauncherTest {

    @Mock
    private Configuration configuration;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private GuiceInjector guiceInjector;
    @Mock
    private SystemOperation systemOperation;
    @InjectMocks
    private PwMan3Launcher pwMan3Launcher;

    @Captor
    private ArgumentCaptor<Module> captor;

    @BeforeEach
    private void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldLaunchMainApplication_IfKeyStoreExists() {
        // given
        final var expectedBootable = mock(PwMan3Application.class);
        setupKeyStoreFileMock(true);
        setupInjectorWithCapture(expectedBootable);

        // when
        pwMan3Launcher.boot();

        // then
        then(expectedBootable).should().boot();
        assertThat(captor.getAllValues()).isNotNull()
                .extracting("class")
                .containsExactly(ApplicationModule.class);
    }

    @Test
    void shouldLaunchSetup_IfKeyStoreDoesNotExist() {
        // given
        final var expectedBootable = mock(PwMan3Setup.class);
        setupKeyStoreFileMock(false);
        setupInjectorWithCapture(expectedBootable);

        // when
        pwMan3Launcher.boot();

        // then
        then(expectedBootable).should().boot();
        assertThat(captor.getAllValues()).isNotNull()
                .extracting("class")
                .containsExactly(SetupModule.class);
    }

    @Test
    void shouldLaunchSetup_IfKeyStoreLocationIsNotSet() {
        // given
        final var expectedBootable = mock(PwMan3Setup.class);
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation("").fake();
        setupInjectorWithCapture(expectedBootable);

        // when
        pwMan3Launcher.boot();

        // then
        then(expectedBootable).should().boot();
        assertThat(captor.getAllValues()).isNotNull()
                .extracting("class")
                .containsExactlyInAnyOrder(SetupModule.class);
    }

    private void setupKeyStoreFileMock(boolean keyStoreExists) {
        final var keystoreDirectory = "/tmp";
        final var keystoreFile = FileFaker.faker()
                .fakeFile()
                .withExistsProperty(keyStoreExists).fake();
        final var path = PathFaker.faker()
                .fakePath()
                .withFileResolvingToFilename(keystoreFile, ReadableConfiguration.KEYSTORE_FILENAME).fake();
        SystemOperationFaker.faker()
                .forInstance(systemOperation)
                .withPath(keystoreDirectory, path).fake();
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation(keystoreDirectory).fake();
    }

    private void setupInjectorWithCapture(final Bootable bootable) {
        final var injector = mock(Injector.class);
        given(injector.getInstance(Bootable.class)).willReturn(bootable);
        given(guiceInjector.create(captor.capture())).willReturn(injector);
    }

}
