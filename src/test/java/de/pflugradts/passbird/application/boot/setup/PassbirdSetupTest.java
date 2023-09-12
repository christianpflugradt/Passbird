package de.pflugradts.passbird.application.boot.setup;

import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.passbird.application.configuration.Configuration;
import de.pflugradts.passbird.application.configuration.ConfigurationSync;
import de.pflugradts.passbird.application.configuration.MockitoConfigurationFaker;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.application.util.FileFaker;
import de.pflugradts.passbird.application.util.PathFaker;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.application.util.SystemOperationFaker;
import de.pflugradts.passbird.domain.model.transfer.Input;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static de.pflugradts.passbird.domain.model.transfer.InputFakerKt.fakeInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PassbirdSetupTest {

    @Spy
    private SetupGuide setupGuide = new SetupGuide(mock(UserInterfaceAdapterPort.class));
    @Mock
    private ConfigurationSync configurationSync;
    @Mock
    private FailureCollector failureCollector;
    @Mock
    private Configuration configuration;
    @Mock
    private KeyStoreAdapterPort keyStoreAdapterPort;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private SystemOperation systemOperation;
    @InjectMocks
    private PassbirdSetup passbirdSetup;

    @Captor
    private ArgumentCaptor<Path> captor;

    @Test
    void shouldRunConfigTemplateRoute() {
        // given
        final var configurationDirectory = "tmp";
        final var password = fakeInput("p4s5w0rD");
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withConfigurationTemplate()
                .withPasswordStoreLocation(configurationDirectory)
                .withKeyStoreLocation(configurationDirectory)
                .fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(password, password)
                .withReceiveConfirmation(true).fake();
        willDoNothing().given(configurationSync).sync(configurationDirectory);
        givenValidDirectory(configurationDirectory);

        // when
        passbirdSetup.boot();

        // then
        then(setupGuide).should().sendWelcome();
        then(setupGuide).should().sendConfigTemplateRouteInformation();
        then(setupGuide).should().sendInputPath("configuration");
        then(setupGuide).should().sendCreateKeyStoreInformation();
        then(setupGuide).should().sendRestart();
        then(setupGuide).should().sendGoodbye();
        then(systemOperation).should().exit();
    }

    @Test
    void shouldAbortUnconfirmedConfigTemplateRoute() {
        // given
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withConfigurationTemplate()
                .fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withReceiveConfirmation(false).fake();

        // when
        passbirdSetup.boot();

        // then
        then(setupGuide).should().sendWelcome();
        then(setupGuide).should().sendConfigTemplateRouteInformation();
        then(setupGuide).should().sendGoodbye();
        then(systemOperation).should().exit();
    }

    @Test
    void shouldRunConfigKeyStoreRoute() {
        // given
        final var configurationDirectory = "tmp";
        final var password = fakeInput("p4s5w0rD");
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation(configurationDirectory)
                .fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(password, password)
                .withReceiveConfirmation(true).fake();
        givenValidDirectory(configurationDirectory);

        // when
        passbirdSetup.boot();

        // then
        then(setupGuide).should().sendWelcome();
        then(setupGuide).should().sendConfigKeyStoreRouteInformation(configurationDirectory);
        then(setupGuide).should().sendInputPath("keystore");
        then(setupGuide).should().sendCreateKeyStoreInformation();
        then(setupGuide).should().sendRestart();
        then(setupGuide).should().sendGoodbye();
        then(systemOperation).should().exit();
    }

    @Test
    void shouldAbortUnconfirmedConfigKeyStoreRoute() {
        // given
        final var configurationDirectory = "tmp";
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation(configurationDirectory)
                .fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withReceiveConfirmation(false).fake();

        // when
        passbirdSetup.boot();

        // then
        then(setupGuide).should().sendWelcome();
        then(setupGuide).should().sendConfigKeyStoreRouteInformation(configurationDirectory);
        then(setupGuide).should().sendGoodbye();
        then(systemOperation).should().exit();
    }

    @Test
    void shouldAcceptCorrectedDirectory() {
        // given
        MockitoAnnotations.initMocks(this);
        final var invalidConfigurationDirectory = "/dev/null";
        final var validDirectory = fakeInput("tmp");
        final var password = fakeInput("p4s5w0rD");
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation(invalidConfigurationDirectory)
                .fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseInputs(validDirectory)
                .withTheseSecureInputs(password, password)
                .withReceiveConfirmation(true).fake();
        doNothing().when(keyStoreAdapterPort).storeKey(any(), captor.capture());
        givenInvalidDirectory(invalidConfigurationDirectory);
        givenValidDirectory(validDirectory.getBytes().asString());

        // when
        passbirdSetup.boot();

        // then
        then(setupGuide).should().sendCreateKeyStoreInformation();
        assertThat(captor.getValue().toString()).startsWith(validDirectory.getBytes().asString());
        then(setupGuide).should().sendRestart();
        then(setupGuide).should().sendGoodbye();
        then(systemOperation).should().exit();
    }

    @Test
    void shouldCreateKeystoreWithMatchingPasswordInput() {
        // given
        final var configurationDirectory = "tmp";
        final var passwordMismatch1 = fakeInput("bassword");
        final var passwordMismatch2 = fakeInput("guessword");
        final var emptyPassword =  Input.Companion.emptyInput();
        final var passwordMatched = fakeInput("p4s5w0rD");
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withKeyStoreLocation(configurationDirectory)
                .fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(
                        passwordMismatch1,
                        passwordMismatch2,
                        emptyPassword,
                        emptyPassword,
                        passwordMatched,
                        passwordMatched)
                .withReceiveConfirmation(true).fake();
        givenValidDirectory(configurationDirectory);

        // when
        passbirdSetup.boot();

        // then
        then(setupGuide).should().sendCreateKeyStoreInformation();
        then(keyStoreAdapterPort).should().storeKey(eq(passwordMatched.getBytes().toChars()), any());
        then(setupGuide).should().sendRestart();
        then(setupGuide).should().sendGoodbye();
        then(systemOperation).should().exit();
    }

    private void givenValidDirectory(final String directory) {
        given(systemOperation.resolvePath(eq(directory), anyString())).willCallRealMethod();
        final var parent = FileFaker.faker()
                .fakeFile()
                .withDirectoryProperty(true)
                .withExistsProperty(true).fake();
        final var file = FileFaker.faker()
                .fakeFile()
                .withDirectoryProperty(true)
                .withParentFile(parent).fake();
        final var path = PathFaker.faker()
                .fakePath()
                .withFileRepresentation(file).fake();
        SystemOperationFaker.faker()
                .forInstance(systemOperation)
                .withPath(directory, path).fake();
    }

    private void givenInvalidDirectory(final String directory) {
        final var file = FileFaker.faker()
                .fakeFile()
                .withDirectoryProperty(false).fake();
        final var path = PathFaker.faker()
                .fakePath()
                .withFileRepresentation(file).fake();
        SystemOperationFaker.faker()
                .forInstance(systemOperation)
                .withPath(directory, path).fake();
    }

}
