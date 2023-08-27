package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler;
import de.pflugradts.passbird.application.license.LicenseManager;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static de.pflugradts.passbird.application.license.LicenseManager.LICENSE_FILENAME;
import static de.pflugradts.passbird.application.license.LicenseManager.THIRD_PARTY_LICENSES_FILENAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class HelpCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private SystemOperation systemOperation;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private LicenseManager licenseManager;
    @InjectMocks
    private HelpCommandHandler helpCommandHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(helpCommandHandler);
    }

    @Test
    void shouldHandleHelpCommand_PrintUsage() {
        // given
        final var input = Input.of(Bytes.of("h"));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains("Usage");
    }

    @Test
    void shouldHandleHelpCommand_OpenLicense() {
        // given
        final var input = Input.of(Bytes.of("hlicense"));

        // when
        inputHandler.handleInput(input);

        // then
        then(systemOperation).should().openFile(eq(new File(LICENSE_FILENAME)));
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains("text editor");
    }

    @Test
    void shouldHandleHelpCommand_Open3rdParty() {
        // given
        final var input = Input.of(Bytes.of("hthirdparty"));

        // when
        inputHandler.handleInput(input);

        // then
        then(systemOperation).should().openFile(eq(new File(THIRD_PARTY_LICENSES_FILENAME)));
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains("web browser");
    }

}
